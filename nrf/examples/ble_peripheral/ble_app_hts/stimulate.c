/*
 * stimulate.c
 *
 *  Created on: Apr 18, 2016
 *      Author: dan
 */

#include <nrf.h>
#ifdef NRF51
#include <nrf_drv_adc.h>
#endif /* NRF51 */
#ifdef NRF52
#include <nrf_drv_saadc.h>
#endif /* NRF52 */
#include <nrf_drv_gpiote.h>
#include <nrf_drv_ppi.h>
#include <nrf_drv_timer.h>
#include <nrf_soc.h>

#include <app_error.h>
#include <app_timer.h>
#include <app_util_platform.h>

#include <ble.h>
#include <ble_gap.h>
#include <ble_gatts.h>

#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

#define NRF_LOG_MODULE_NAME "STIM"
#include "nrf_log.h"
#include "nrf_log_ctrl.h"


typedef enum {
	TIMER_STOPPED = 0,
	TIMER_RUNNING = 1,
} timer_state_t;


static void stimulate_service_add_characteristic(ble_uuid_t*, ble_gatts_char_handles_t*, void*, uint8_t);
static void stimulate_service_on_gatts_write_event(ble_gatts_evt_write_t*);
static void stimulate_service_on_gatts_write_activ_params(void);
static void stimulate_service_on_gatts_write_round_params(void);
static void stimulate_service_on_gatts_write_pulse_params(void);

static void activ_start(void);
static void activ_stop(void);
static void activ_handle_timer_event(void*);

static void round_start_on(void);
static void round_start_off(void);
static void round_stop(void);
static void round_handle_timer_event(nrf_timer_event_t, void*);
static uint32_t round_compute_on_timer_count(uint32_t*);
static uint32_t round_compute_off_timer_count(uint32_t*);
static uint16_t round_compute_timer_compare(uint32_t*);

static void pulse_start(void);
static void pulse_stop(void);
static void pulse_update(uint8_t);
static void pulse_config_pin(nrf_drv_gpiote_pin_t, nrf_timer_event_t, nrf_timer_event_t);
static void pulse_handle_timer_event(nrf_timer_event_t, void*);
static void pulse_compute_timer_config(void);
static uint16_t pulse_compute_timer_compare(uint32_t*);


#if defined(BOARD_PCA10028) || defined(BOARD_SPARROW_BLE)
static const uint32_t enable_pin = 5;		// A4
static const uint32_t positive_pin = 3;		// A2
static const uint32_t negative_pin = 4;		// A3
#elif defined(BOARD_PCA10031)
static const uint32_t enable_pin = 18;		// 18
static const uint32_t positive_pin = 19;	// 19
static const uint32_t negative_pin = 20;	// 20
#elif defined(BOARD_PCA10040)
static const uint32_t enable_pin = 30;		// A4
static const uint32_t positive_pin = 28;	// A2
static const uint32_t negative_pin = 29;	// A3
#elif defined(BOARD_TOOTH)
static const uint32_t stimulation_pin = 0;
#endif

static const ble_uuid128_t full_uuid = {{0x09, 0x45, 0x37, 0x13, 0xc3, 0xe6, 0x79, 0xb5, 0x8c, 0x41, 0xaf, 0x2e, 0x81, 0xbc, 0x6f, 0x01}};
static ble_uuid_t service_uuid;
static ble_uuid_t stimulate_uuid;
static ble_uuid_t t_total_uuid;
static ble_uuid_t t_pulse_uuid;
static ble_uuid_t t_pause_uuid;
static ble_uuid_t t_positive_pulse_uuid;
static ble_uuid_t t_positive_pause_uuid;
static ble_uuid_t t_negative_pulse_uuid;
static ble_uuid_t t_negative_pause_uuid;
static ble_uuid_t amplitude_uuid;

static uint16_t service_handle;
static ble_gatts_char_handles_t stimulate_handle;
static ble_gatts_char_handles_t t_total_handle;
static ble_gatts_char_handles_t t_pulse_handle;
static ble_gatts_char_handles_t t_pause_handle;
static ble_gatts_char_handles_t t_positive_pulse_handle;
static ble_gatts_char_handles_t t_positive_pause_handle;
static ble_gatts_char_handles_t t_negative_pulse_handle;
static ble_gatts_char_handles_t t_negative_pause_handle;
static ble_gatts_char_handles_t amplitude_handle;

static uint8_t stimulate_value = 0;
static uint8_t stimulate_prev = 0;
static uint32_t t_total_value = 0;
static uint32_t t_pulse_value = 0;
static uint32_t t_pause_value = 0;
static uint32_t t_positive_pulse_value = 0;
static uint32_t t_positive_pause_value = 0;
static uint32_t t_negative_pulse_value = 0;
static uint32_t t_negative_pause_value = 0;
static uint32_t amplitude_value = 0;

static uint16_t conn_handle = BLE_CONN_HANDLE_INVALID;

// must update compute_timer_compare when changing timer configuration
static const nrf_drv_timer_t stimulate_timer = NRF_DRV_TIMER_INSTANCE(1);
static const nrf_drv_timer_config_t stimulate_timer_cfg = NRF_DRV_TIMER_DEFAULT_CONFIG;
static volatile timer_state_t pulse_timer_state;
static nrf_ppi_channel_t pulse_stop_channel;
static volatile uint16_t compare0;
static volatile uint16_t compare1;
static volatile uint16_t compare2;
static volatile uint16_t compare3;

static const nrf_drv_timer_t train_timer = NRF_DRV_TIMER_INSTANCE(2);
static volatile timer_state_t train_timer_state;
static uint32_t train_timer_remaining;

APP_TIMER_DEF(timer);


void stimulate_service_init(void)
{
	uint32_t err_code;

	// add service UUID
	service_uuid.uuid = full_uuid.uuid128[13] << 8 | full_uuid.uuid128[12];
	err_code = sd_ble_uuid_vs_add(&full_uuid, &service_uuid.type);
	APP_ERROR_CHECK(err_code);

	// add stimulate command characteristic UUID
	stimulate_uuid.uuid = 0x0000;
	stimulate_uuid.type = service_uuid.type;

	// add total time characteristic UUID
	t_total_uuid.uuid = 0x0001;
	t_total_uuid.type = service_uuid.type;

	// add stimulation on time characteristic UUID
	t_pulse_uuid.uuid = 0x0002;
	t_pulse_uuid.type = service_uuid.type;

	// add stimulation pause time characteristic UUID
	t_pause_uuid.uuid = 0x0003;
	t_pause_uuid.type = service_uuid.type;

	// add positive pulse time characteristic UUID
	t_positive_pulse_uuid.uuid = 0x0004;
	t_positive_pulse_uuid.type = service_uuid.type;

	// add pause time after positive pulse characteristic UUID
	t_positive_pause_uuid.uuid = 0x0005;
	t_positive_pause_uuid.type = service_uuid.type;

	// add negative pulse time characteristic UUID
	t_negative_pulse_uuid.uuid = 0x0006;
	t_negative_pulse_uuid.type = service_uuid.type;

	// add pause time after negative pulse characteristic UUID
	t_negative_pause_uuid.uuid = 0x0007;
	t_negative_pause_uuid.type = service_uuid.type;

	// add pulse amplitude characteristic UUID
	amplitude_uuid.uuid = 0x0008;
	amplitude_uuid.type = service_uuid.type;

	// add service
	err_code = sd_ble_gatts_service_add(BLE_GATTS_SRVC_TYPE_PRIMARY, &service_uuid, &service_handle);
	APP_ERROR_CHECK(err_code);

	// add characteristics
	stimulate_service_add_characteristic(&stimulate_uuid, &stimulate_handle, &stimulate_value, sizeof(stimulate_value));
	stimulate_service_add_characteristic(&t_total_uuid, &t_total_handle, &t_total_value, sizeof(t_total_value));
	stimulate_service_add_characteristic(&t_pulse_uuid, &t_pulse_handle, &t_pulse_value, sizeof(t_pulse_value));
	stimulate_service_add_characteristic(&t_pause_uuid, &t_pause_handle, &t_pause_value, sizeof(t_pause_value));
	stimulate_service_add_characteristic(&t_positive_pulse_uuid, &t_positive_pulse_handle, &t_positive_pulse_value, sizeof(t_positive_pulse_value));
	stimulate_service_add_characteristic(&t_positive_pause_uuid, &t_positive_pause_handle, &t_positive_pause_value, sizeof(t_positive_pause_value));
	stimulate_service_add_characteristic(&t_negative_pulse_uuid, &t_negative_pulse_handle, &t_negative_pulse_value, sizeof(t_negative_pulse_value));
	stimulate_service_add_characteristic(&t_negative_pause_uuid, &t_negative_pause_handle, &t_negative_pause_value, sizeof(t_negative_pause_value));
	stimulate_service_add_characteristic(&amplitude_uuid, &amplitude_handle, &amplitude_value, sizeof(amplitude_value));

	// init stimulation timer
	err_code = app_timer_create(&timer, APP_TIMER_MODE_SINGLE_SHOT, activ_handle_timer_event);
	APP_ERROR_CHECK(err_code);

	// init GPIO task and events module
	if(!nrf_drv_gpiote_is_init())
	{
		err_code = nrf_drv_gpiote_init();
		APP_ERROR_CHECK(err_code);
	}

	// init programmable peripheral interconnect
	err_code = nrf_drv_ppi_init();
	APP_ERROR_CHECK(err_code);

	// init pulse generation timer module
	err_code = nrf_drv_timer_init(&stimulate_timer, &stimulate_timer_cfg, pulse_handle_timer_event);
	APP_ERROR_CHECK(err_code);

	// configure electrode(s) when GPIOTE is inactive
#if defined(BOARD_PCA10028) || defined(BOARD_PCA10031) || defined(BOARD_PCA10040) || defined(BOARD_SPARROW_BLE)
	// default state of H-bridge is disabled
	NRF_GPIO->OUTCLR = (1 << positive_pin) | (1 << negative_pin);
	NRF_GPIO->DIRSET = (1 << positive_pin) | (1 << negative_pin);
#elif defined(BOARD_TOOTH)
	// default state of stimulation pin is disabled
	NRF_GPIO->OUTCLR = (1 << stimulation_pin);
	NRF_GPIO->DIRSET = (1 << stimulation_pin);
#endif

#if defined(BOARD_PCA10028) || defined(BOARD_PCA10031) || defined(BOARD_PCA10040) || defined(BOARD_SPARROW_BLE)
	// configure stimulation voltage source control, disabling the source
	NRF_GPIO->OUTCLR = (1 << enable_pin);
	NRF_GPIO->DIRSET = (1 << enable_pin);
#endif

	// configure electrode(s) when under the control of GPIOTE
#if defined(BOARD_PCA10028) || defined(BOARD_PCA10031) || defined(BOARD_PCA10040) || defined(BOARD_SPARROW_BLE)
	// H-bridge controlled by timer events
	pulse_config_pin((nrf_drv_gpiote_pin_t)positive_pin, NRF_TIMER_EVENT_COMPARE0, NRF_TIMER_EVENT_COMPARE1);
	pulse_config_pin((nrf_drv_gpiote_pin_t)negative_pin, NRF_TIMER_EVENT_COMPARE2, NRF_TIMER_EVENT_COMPARE3);
#elif defined(BOARD_TOOTH)
	// stimulation pin controlled by timer events
	pulse_config_pin((nrf_drv_gpiote_pin_t)stimulation_pin, NRF_TIMER_EVENT_COMPARE0, NRF_TIMER_EVENT_COMPARE1);
#endif

	// configure channel used for pulse counting
	nrf_ppi_channel_t pulse_count_channel;
	uint32_t pulse_count_event_addr = nrf_drv_timer_event_address_get(&stimulate_timer, NRF_TIMER_EVENT_COMPARE3);
	uint32_t pulse_count_task_addr = nrf_drv_timer_task_address_get(&train_timer, NRF_TIMER_TASK_COUNT);
	APP_ERROR_CHECK(nrf_drv_ppi_channel_alloc(&pulse_count_channel));
	APP_ERROR_CHECK(nrf_drv_ppi_channel_assign(pulse_count_channel, pulse_count_event_addr, pulse_count_task_addr));
	APP_ERROR_CHECK(nrf_drv_ppi_channel_enable(pulse_count_channel));

	// configure channel used for pulse stopping
	uint32_t pulse_stop_event_addr = nrf_drv_timer_event_address_get(&train_timer, NRF_TIMER_EVENT_COMPARE0);
	uint32_t pulse_stop_task_addr = nrf_drv_timer_task_address_get(&stimulate_timer, NRF_TIMER_TASK_STOP);
	APP_ERROR_CHECK(nrf_drv_ppi_channel_alloc(&pulse_stop_channel));
	APP_ERROR_CHECK(nrf_drv_ppi_channel_assign(pulse_stop_channel, pulse_stop_event_addr, pulse_stop_task_addr));
}

void stimulate_service_on_ble_event(ble_evt_t* event)
{
	switch(event->header.evt_id)
	{
		case BLE_GAP_EVT_CONNECTED:
			conn_handle = event->evt.gap_evt.conn_handle;
			break;

		case BLE_GAP_EVT_DISCONNECTED:
			conn_handle = BLE_CONN_HANDLE_INVALID;
			break;

		case BLE_GATTS_EVT_WRITE:
			stimulate_service_on_gatts_write_event(&event->evt.gatts_evt.params.write);
			break;
	}
}

static void stimulate_service_add_characteristic(
		ble_uuid_t* char_uuid,
		ble_gatts_char_handles_t* char_handle,
		void* value, uint8_t length)
{
	ble_gatts_attr_md_t attr_md;
	ble_gatts_char_md_t char_md;
	ble_gatts_attr_t attr;

	// characteristic metadata
	memset(&char_md, 0, sizeof(char_md));
	char_md.char_props.read		= 1;
	char_md.char_props.write	= 1;
	char_md.char_props.notify	= 1;
	char_md.p_char_user_desc	= NULL;
	char_md.p_char_pf			= NULL;
	char_md.p_user_desc_md		= NULL;
	char_md.p_cccd_md			= NULL;
	char_md.p_sccd_md			= NULL;

	// attribute metadata
	memset(&attr_md, 0, sizeof(attr_md));
	BLE_GAP_CONN_SEC_MODE_SET_OPEN(&attr_md.read_perm);
	BLE_GAP_CONN_SEC_MODE_SET_OPEN(&attr_md.write_perm);
	attr_md.vloc = BLE_GATTS_VLOC_USER;

	// attribute
	memset(&attr, 0, sizeof(attr));
	attr.p_uuid		= char_uuid;
	attr.p_attr_md	= &attr_md;
	attr.init_len	= length;
	attr.init_offs	= 0;
	attr.max_len	= attr.init_len;
	attr.p_value	= value;

	uint32_t err_code = sd_ble_gatts_characteristic_add(service_handle, &char_md, &attr, char_handle);
	APP_ERROR_CHECK(err_code);
}

static void stimulate_service_on_gatts_write_event(ble_gatts_evt_write_t* event)
{
	if(event->offset != 0)
	{
		NRF_LOG_DEBUG("stimulate (%hx): write with non-zero offset %zd not supported\n", event->uuid.uuid, event->offset);
		return;
	}

	switch(event->uuid.uuid)
	{
		case 0x0000:
			if(event->len == 1)
				break;
			goto bad_length;

		case 0x0001:
		case 0x0002:
		case 0x0003:
		case 0x0004:
		case 0x0005:
		case 0x0006:
		case 0x0007:
			if(event->len == 4)
				break;
			goto bad_length;

		default:
		bad_length:
			NRF_LOG_DEBUG("stimulate (%hx): write of length %hd not supported\n", event->uuid.uuid, event->len);
			return;
	}

	// uniformly get data written on event
	uint32_t value_data;
	ble_gatts_value_t value = {
			.len = 4,
			.offset = 0,
			.p_value = (uint8_t*)&value_data,
	};
	sd_ble_gatts_value_get(BLE_CONN_HANDLE_INVALID, event->handle, &value);

	switch(event->uuid.uuid)
	{
		case 0x0000:
			NRF_LOG_INFO("stimulate (%hx): write value %hu\n", event->uuid.uuid, *((uint8_t*)&value_data));
			stimulate_service_on_gatts_write_activ_params();
			break;

		case 0x0001:
			NRF_LOG_INFO("stimulate (%hx): write value %lu\n", event->uuid.uuid, value_data);
			break;

		case 0x0002:
		case 0x0003:
			NRF_LOG_INFO("stimulate (%hx): write value %lu\n", event->uuid.uuid, value_data);
			stimulate_service_on_gatts_write_round_params();
			break;

		case 0x0004:
		case 0x0005:
		case 0x0006:
		case 0x0007:
			NRF_LOG_INFO("stimulate (%hx): write value %lu\n", event->uuid.uuid, value_data);
			stimulate_service_on_gatts_write_pulse_params();
			break;

		case 0x0008:
			NRF_LOG_INFO("stimulate (%hx): write value %lu\n", event->uuid.uuid, value_data);
			break;
	}
}

static void stimulate_service_on_gatts_write_activ_params(void)
{
	// normalize to boolean
	stimulate_value = !!stimulate_value;

	// starting and stopping are not idempotent
	// only call them on state transition
	if(stimulate_value && !stimulate_prev)
	{
		activ_start();

		uint32_t ticks = APP_TIMER_TICKS(t_total_value / 1000, 0);
		if(ticks > 0 && ticks < APP_TIMER_MIN_TIMEOUT_TICKS)
			ticks = APP_TIMER_MIN_TIMEOUT_TICKS;
		if(ticks > 0)
			APP_ERROR_CHECK(app_timer_start(timer, ticks, NULL));
	}
	else if(!stimulate_value && stimulate_prev)
	{
		activ_stop();

		APP_ERROR_CHECK(app_timer_stop(timer));
	}
	stimulate_prev = stimulate_value;
}

static void stimulate_service_on_gatts_write_round_params(void)
{
	// params are updated automatically at the start of each pulse/pause phase
}

static void stimulate_service_on_gatts_write_pulse_params(void)
{
	pulse_compute_timer_config();
}

static void activ_start(void)
{
#if defined(BOARD_PCA10028) || defined(BOARD_PCA10031) || defined(BOARD_PCA10040) || defined(BOARD_SPARROW_BLE)
	// enable stimulation voltage source
	NRF_GPIO->OUTSET = (1 << enable_pin);
#endif

	round_start_on();
}

static void activ_stop(void)
{
	pulse_stop();

	round_stop();

#if defined(BOARD_PCA10028) || defined(BOARD_PCA10031) || defined(BOARD_PCA10040) || defined(BOARD_SPARROW_BLE)
	// disable stimulation voltage source
	NRF_GPIO->OUTCLR = (1 << enable_pin);
#endif
}

static void activ_handle_timer_event(void* context)
{
	activ_stop();
	stimulate_prev = stimulate_value = 0;
}

static void round_start_on(void)
{
	// clean-up previous timer mode
	nrf_drv_timer_uninit(&train_timer);

	// if no pulses are configured skip pulse phase
	if(t_pulse_value == 0)
	{
		round_start_off();
		return;
	}

	pulse_update(pulse_timer_state);

	// compute total remaining timer counts and next compare threshold
	train_timer_remaining = round_compute_on_timer_count(&t_pulse_value);
	uint16_t timer_next_compare = round_compute_timer_compare(&train_timer_remaining);

	// set timer configuration for generating pulse train
	nrf_drv_timer_config_t timer_cfg = NRF_DRV_TIMER_DEFAULT_CONFIG;
	timer_cfg.mode = TIMER_MODE_MODE_Counter;
	timer_cfg.p_context = round_start_off;

	// init, configure and start timer
	APP_ERROR_CHECK(nrf_drv_timer_init(&train_timer, &timer_cfg, round_handle_timer_event));
	if(train_timer_remaining == 0)
	{
		APP_ERROR_CHECK(nrf_drv_ppi_channel_enable(pulse_stop_channel));
		nrf_drv_timer_extended_compare(&train_timer,
				NRF_TIMER_CC_CHANNEL0, timer_next_compare,
				NRF_TIMER_SHORT_COMPARE0_CLEAR_MASK | NRF_TIMER_SHORT_COMPARE0_STOP_MASK, true);
	}
	else
	{
		APP_ERROR_CHECK(nrf_drv_ppi_channel_disable(pulse_stop_channel));
		nrf_drv_timer_extended_compare(&train_timer,
				NRF_TIMER_CC_CHANNEL0, timer_next_compare,
				NRF_TIMER_SHORT_COMPARE0_CLEAR_MASK, true);
	}
	train_timer_state = TIMER_RUNNING;
	nrf_drv_timer_enable(&train_timer);

	pulse_start();
}

static void round_start_off(void)
{
	// clean-up previous timer mode
	nrf_drv_timer_uninit(&train_timer);

	// if no pause is configured force minimum possible
	if(t_pause_value == 0)
		t_pause_value = 1;

	// compute total remaining timer counts and next compare threshold
	train_timer_remaining = round_compute_off_timer_count(&t_pause_value);
	uint16_t timer_next_compare = round_compute_timer_compare(&train_timer_remaining);

	// set timer configuration for generating pause period
	nrf_drv_timer_config_t timer_cfg = NRF_DRV_TIMER_DEFAULT_CONFIG;
	timer_cfg.mode = TIMER_MODE_MODE_Timer;
	timer_cfg.p_context = round_start_on;

	// init, configure and start timer
	APP_ERROR_CHECK(nrf_drv_timer_init(&train_timer, &timer_cfg, round_handle_timer_event));
	if(train_timer_remaining == 0)
	{
		nrf_drv_timer_extended_compare(&train_timer,
				NRF_TIMER_CC_CHANNEL0, timer_next_compare,
				NRF_TIMER_SHORT_COMPARE0_CLEAR_MASK | NRF_TIMER_SHORT_COMPARE0_STOP_MASK, true);
	}
	else
	{
		nrf_drv_timer_extended_compare(&train_timer,
				NRF_TIMER_CC_CHANNEL0, timer_next_compare,
				NRF_TIMER_SHORT_COMPARE0_CLEAR_MASK, true);
	}
	train_timer_state = TIMER_RUNNING;
	nrf_drv_timer_enable(&train_timer);
}

static void round_stop(void)
{
	if(train_timer_state == TIMER_RUNNING)
	{
		nrf_drv_timer_disable(&train_timer);
		train_timer_state = TIMER_STOPPED;
	}
}

static void round_handle_timer_event(nrf_timer_event_t event_type, void* context)
{
	switch(event_type)
	{
		case NRF_TIMER_EVENT_COMPARE0:
			if(train_timer_remaining)
			{
				uint16_t timer_next_compare = round_compute_timer_compare(&train_timer_remaining);
				nrf_drv_timer_compare(&train_timer, NRF_TIMER_CC_CHANNEL0, timer_next_compare, true);

				if(train_timer_remaining == 0)
				{
					APP_ERROR_CHECK(nrf_drv_ppi_channel_enable(pulse_stop_channel));
					nrf_timer_shorts_enable(train_timer.p_reg, NRF_TIMER_SHORT_COMPARE0_STOP_MASK);
				}
			}
			else
			{
				nrf_drv_timer_disable(&train_timer);
				train_timer_state = TIMER_STOPPED;

				void (*start_next_timer)() = context;

				if(start_next_timer == round_start_off)
					pulse_stop();
				start_next_timer();
			}
			break;

		default:
			break;
	}
}

static uint32_t round_compute_on_timer_count(uint32_t* value)
{
	const uint32_t period = t_positive_pause_value + t_positive_pulse_value + t_negative_pause_value + t_negative_pulse_value;

	if(*value == 0)
		return 0;

	if(*value < period)
	{
		*value = period;
		return 1;
	}

	*value = (*value / period) * period;
	return *value / period;
}

static uint32_t round_compute_off_timer_count(uint32_t* value)
{
	const uint32_t period = 1000000 / (16000000 / (1 << 9));

	if(*value == 0)
		return 0;

	if(*value < period)
	{
		*value = period;
		return 1;
	}

	*value = (*value / period) * period;
	return *value / period;
}

static uint16_t round_compute_timer_compare(uint32_t* remaining)
{
	if(*remaining == 0)
		APP_ERROR_HANDLER(0xdead0001);

	uint16_t ret;

	// maximize the compare value so that we don't try to measure very small
	// time intervals while the timer is running (STOP shortcut is disabled)
	// this is done by equally dividing an interval between the current compare
	// threshold and the next when possible
	if(*remaining < (1 << 16))
	{
		ret = *remaining;
		*remaining = 0;
	}
	else if(*remaining < 2 * (1 << 16))
	{
		ret = *remaining / 2;
		*remaining -= ret;
	}
	else
	{
		ret = 0;
		*remaining -= (1 << 16);
	}

	return ret;
}

static void pulse_start(void)
{
	// protect against concurrent execution from scheduler or train_timer handler
	CRITICAL_REGION_ENTER();

	if(pulse_timer_state == TIMER_RUNNING)
		goto end;

	// make sure toggling starts with electrode(s) disabled
#if defined(BOARD_PCA10028) || defined(BOARD_PCA10031) || defined(BOARD_PCA10040) || defined(BOARD_SPARROW_BLE)
	// H-bridge is disabled
	nrf_drv_gpiote_out_task_force((nrf_drv_gpiote_pin_t)positive_pin, 0);
	nrf_drv_gpiote_out_task_force((nrf_drv_gpiote_pin_t)negative_pin, 0);
#elif defined(BOARD_TOOTH)
	// stimulation pin is disabled
	nrf_drv_gpiote_out_task_force((nrf_drv_gpiote_pin_t)stimulation_pin, 0);
#endif

	pulse_timer_state = TIMER_RUNNING;
	nrf_drv_timer_enable(&stimulate_timer);

end:
	CRITICAL_REGION_EXIT();
}

static void pulse_stop(void)
{
	// protect against concurrent execution from scheduler or train_timer handler
	CRITICAL_REGION_ENTER();

	if(pulse_timer_state == TIMER_STOPPED)
		goto end;

	// disable timer
	nrf_drv_timer_disable(&stimulate_timer);
	pulse_timer_state = TIMER_STOPPED;

	// make sure electrode(s) are disabled
#if defined(BOARD_PCA10028) || defined(BOARD_PCA10031) || defined(BOARD_PCA10040) || defined(BOARD_SPARROW_BLE)
	// H-bridge is disabled
	nrf_drv_gpiote_out_task_disable((nrf_drv_gpiote_pin_t)positive_pin);
	nrf_drv_gpiote_out_task_disable((nrf_drv_gpiote_pin_t)negative_pin);
#elif defined(BOARD_TOOTH)
	// stimulation pin is disabled
	nrf_drv_gpiote_out_task_disable((nrf_drv_gpiote_pin_t)stimulation_pin);
#endif

end:
	CRITICAL_REGION_EXIT();
}

static void pulse_update(timer_state_t timer_state)
{
	// protect against concurrent execution from scheduler or train_timer handler
	CRITICAL_REGION_ENTER();

	switch(timer_state)
	{
		case TIMER_STOPPED:
			// update pulse configuration
			nrf_drv_timer_compare(&stimulate_timer, NRF_TIMER_CC_CHANNEL0, compare0, false);
			nrf_drv_timer_compare(&stimulate_timer, NRF_TIMER_CC_CHANNEL1, compare1, false);
			nrf_drv_timer_compare(&stimulate_timer, NRF_TIMER_CC_CHANNEL2, compare2, false);
			nrf_drv_timer_extended_compare(&stimulate_timer, NRF_TIMER_CC_CHANNEL3, compare3, NRF_TIMER_SHORT_COMPARE3_CLEAR_MASK, false);

			// pulse pin toggling doesn't work if duration is 0, replace with task disabling
#if defined(BOARD_PCA10028) || defined(BOARD_PCA10031) || defined(BOARD_PCA10040) || defined(BOARD_SPARROW_BLE)
			if(compare0 != compare1)
				nrf_drv_gpiote_out_task_enable((nrf_drv_gpiote_pin_t)positive_pin);
			else
				nrf_drv_gpiote_out_task_disable((nrf_drv_gpiote_pin_t)positive_pin);
			if(compare2 != compare3)
				nrf_drv_gpiote_out_task_enable((nrf_drv_gpiote_pin_t)negative_pin);
			else
				nrf_drv_gpiote_out_task_disable((nrf_drv_gpiote_pin_t)negative_pin);
#elif defined(BOARD_TOOTH)
			if(compare0 != compare1)
				nrf_drv_gpiote_out_task_enable((nrf_drv_gpiote_pin_t)stimulation_pin);
			else
				nrf_drv_gpiote_out_task_disable((nrf_drv_gpiote_pin_t)stimulation_pin);
#endif
			break;

		default:
			ASSERT(0);
			break;
	}

	CRITICAL_REGION_EXIT();
}

static void pulse_config_pin(nrf_drv_gpiote_pin_t pin,
		nrf_timer_event_t first_toggle_event,
		nrf_timer_event_t second_toggle_event)
{
	// configure GPIOTE, pin action is toggle, initial value is LOW
	nrf_drv_gpiote_out_config_t toggle_cfg = GPIOTE_CONFIG_OUT_TASK_TOGGLE(0);
	APP_ERROR_CHECK(nrf_drv_gpiote_out_init(pin, &toggle_cfg));

	// configure first toggle PPI channel
	nrf_ppi_channel_t first_toggle_channel;
	uint32_t first_event_addr = nrf_drv_timer_event_address_get(&stimulate_timer, first_toggle_event);
	uint32_t first_task_addr = nrf_drv_gpiote_out_task_addr_get(pin);
	APP_ERROR_CHECK(nrf_drv_ppi_channel_alloc(&first_toggle_channel));
	APP_ERROR_CHECK(nrf_drv_ppi_channel_assign(first_toggle_channel, first_event_addr, first_task_addr));

	// configure second toggle PPI channel
	nrf_ppi_channel_t second_toggle_channel;
	uint32_t second_event_addr = nrf_drv_timer_event_address_get(&stimulate_timer, second_toggle_event);
	uint32_t second_task_addr = nrf_drv_gpiote_out_task_addr_get(pin);
	APP_ERROR_CHECK(nrf_drv_ppi_channel_alloc(&second_toggle_channel));
	APP_ERROR_CHECK(nrf_drv_ppi_channel_assign(second_toggle_channel, second_event_addr, second_task_addr));

	// activate channels
	APP_ERROR_CHECK(nrf_drv_ppi_channel_enable(first_toggle_channel));
	APP_ERROR_CHECK(nrf_drv_ppi_channel_enable(second_toggle_channel));
	nrf_drv_gpiote_out_task_enable(pin);
}

static void pulse_handle_timer_event(nrf_timer_event_t event_type, void* p_context)
{
}

static void pulse_compute_timer_config(void)
{
	// protect against concurrent execution with pulse_update
	CRITICAL_REGION_ENTER();

	// positive pulse
	if(t_positive_pause_value == 0 && t_positive_pulse_value != 0)	// no pulse without pause
		t_positive_pause_value++;
	compare0 = pulse_compute_timer_compare(&t_positive_pause_value) + 0;
	compare1 = pulse_compute_timer_compare(&t_positive_pulse_value) + compare0;

	// negative pulse
	if(t_negative_pause_value == 0 && t_negative_pulse_value != 0)	// no pulse without pause
		t_negative_pause_value++;
	compare2 = pulse_compute_timer_compare(&t_negative_pause_value) + compare1;
	compare3 = pulse_compute_timer_compare(&t_negative_pulse_value) + compare2;

	CRITICAL_REGION_EXIT();
}

static uint16_t pulse_compute_timer_compare(uint32_t* value)
{
	const uint32_t min_value = 1000000 / (16000000 / (1 << stimulate_timer_cfg.frequency));
	const uint32_t max_value = ((1 << 16) * min_value) / 4;

	if(*value == 0)
		return 0;

	if(*value < min_value)
	{
		*value = min_value;
		return 1;
	}

	if(*value > max_value)
	{
		*value = max_value;
		return max_value / min_value;
	}

	*value = (*value / min_value) * min_value;
	return *value / min_value;
}

uint8_t stimulate_measurement_init(uint8_t adc_channel)
{
#ifdef NRF51
	static nrf_drv_adc_channel_t channel = {
			.config.config = {
				.resolution = NRF_ADC_CONFIG_RES_10BIT,
				.input = NRF_ADC_CONFIG_SCALING_INPUT_ONE_THIRD,
				.reference = NRF_ADC_CONFIG_REF_VBG,	// 1.2V
				.ain = NRF_ADC_CONFIG_INPUT_3,
			},
	};
	nrf_drv_adc_channel_enable(&channel);
	adc_channel++;
#endif /* NRF51 */

#ifdef NRF52
	ret_code_t err_code;

	static nrf_saadc_channel_config_t channel = {
			.resistor_p = NRF_SAADC_RESISTOR_DISABLED,
			.resistor_n = NRF_SAADC_RESISTOR_DISABLED,
			.gain = NRF_SAADC_GAIN1_6,
			.reference = NRF_SAADC_REFERENCE_INTERNAL,	// 0.6V
			.acq_time = NRF_SAADC_ACQTIME_10US,
			.mode = NRF_SAADC_MODE_SINGLE_ENDED,
			.burst = NRF_SAADC_BURST_DISABLED,
			.pin_p = NRF_SAADC_INPUT_VDD,
			.pin_n = NRF_SAADC_INPUT_DISABLED,
	};
	err_code = nrf_drv_saadc_channel_init(adc_channel++, &channel);
	APP_ERROR_CHECK(err_code);
#endif /* NRF52 */

	return adc_channel;
}

uint8_t stimulate_measurement_sample(int16_t* sample)
{
#ifdef NRF51
	float amplitude_voltage = *sample * (1.2f / 1024 / (1.0f/3) / (1.2f / (1.2f + 15.0f))); // REF=1.2V, RES=10bit, GAIN=1/3, VDD_RES=15K, GND_RES=1.2K
	amplitude_value = amplitude_voltage * 1000;	//mV
#endif /* NRF51 */

#ifdef NRF52
	float amplitude_voltage = *sample * (0.6f / 1024 / (1.0f/6));	// REF=0.6V, RES=10bit, GAIN=1/6
	amplitude_value = amplitude_voltage * 1000;	//mV
#endif /* NRF52 */

	if(conn_handle != BLE_CONN_HANDLE_INVALID)
	{
		uint16_t len = sizeof(amplitude_value);

		ble_gatts_hvx_params_t params;
		memset(&params, 0, sizeof(params));
		params.type = BLE_GATT_HVX_NOTIFICATION;
		params.handle = amplitude_handle.value_handle;
		params.p_data = NULL;
		params.p_len = &len;

		uint32_t err_code = sd_ble_gatts_hvx(conn_handle, &params);
		if(	(err_code != NRF_SUCCESS) &&
			(err_code != NRF_ERROR_BUSY) &&
			(err_code != NRF_ERROR_INVALID_STATE) &&
			(err_code != BLE_ERROR_NO_TX_PACKETS) &&
			(err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING))
		{
			APP_ERROR_HANDLER(err_code);
		}
	}

	return 1;
}
