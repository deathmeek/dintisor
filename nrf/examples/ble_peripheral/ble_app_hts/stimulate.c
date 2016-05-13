/*
 * stimulate.c
 *
 *  Created on: Apr 18, 2016
 *      Author: dan
 */

#include <nrf.h>
#include <nrf_drv_gpiote.h>
#include <nrf_drv_ppi.h>
#include <nrf_drv_timer.h>
#include <nrf_soc.h>

#include <app_error.h>
#include <app_util_platform.h>

#include <ble.h>
#include <ble_gap.h>
#include <ble_gatts.h>

#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>


static void stimulate_on_gatts_write_stimulate(void);
static void stimulate_on_gatts_write_pulse_params(void);

static void stimulate_compute_config(void);
static void stimulate_update_config(uint8_t);
static void stimulate_start(void);
static void stimulate_stop(void);
static void stimulate_timer_event(nrf_timer_event_t, void*);

static uint16_t compute_timer_compare(uint32_t*);


static const uint32_t enable_pin = 5;
static const uint32_t positive_pin = 3;
static const uint32_t negative_pin = 4;

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
static const nrf_drv_timer_config_t stimulate_timer_cfg = NRF_DRV_TIMER_DEFAULT_CONFIG(1);
static volatile uint16_t compare0;
static volatile uint16_t compare1;
static volatile uint16_t compare2;
static volatile uint16_t compare3;


static uint16_t compute_timer_compare(uint32_t* value)
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

static void setup_pulse_pin(nrf_drv_gpiote_pin_t pin,
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

void stimulate_service_add_characteristic(
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

	// init GPIO task and events module
	if(!nrf_drv_gpiote_is_init())
	{
		err_code = nrf_drv_gpiote_init();
		APP_ERROR_CHECK(err_code);
	}

	// init programmable peripheral interconnect
	err_code = nrf_drv_ppi_init();
	APP_ERROR_CHECK(err_code);

	// init timer module
	err_code = nrf_drv_timer_init(&stimulate_timer, &stimulate_timer_cfg, stimulate_timer_event);
	APP_ERROR_CHECK(err_code);

	// configure H-bridge controls when GPIOTE is inactive
	// default state of H-bridge is disabled
	NRF_GPIO->OUTCLR = (1 << positive_pin) | (1 << negative_pin);
	NRF_GPIO->DIRSET = (1 << positive_pin) | (1 << negative_pin);

	// configure stimulation voltage source control, disabling the source
	NRF_GPIO->OUTCLR = (1 << enable_pin);
	NRF_GPIO->DIRSET = (1 << enable_pin);

	// configure H-bridge controls when under the control of GPIOTE
	setup_pulse_pin((nrf_drv_gpiote_pin_t)positive_pin, NRF_TIMER_EVENT_COMPARE0, NRF_TIMER_EVENT_COMPARE1);
	setup_pulse_pin((nrf_drv_gpiote_pin_t)negative_pin, NRF_TIMER_EVENT_COMPARE2, NRF_TIMER_EVENT_COMPARE3);
}

void stimulate_on_gatts_write_event(ble_gatts_evt_write_t* event)
{
	if(event->offset != 0)
	{
		printf("stimulate (%hx): write with non-zero offset %zd not supported\n", event->context.char_uuid.uuid, event->offset);
		return;
	}

	switch(event->context.char_uuid.uuid)
	{
		case 0x0000:
			if(event->len == 1)
				break;
			/* no break */

		case 0x0001:
		case 0x0002:
		case 0x0003:
		case 0x0004:
		case 0x0005:
		case 0x0006:
		case 0x0007:
			if(event->len == 4)
				break;
			/* no break */

		default:
			printf("stimulate (%hx): write of length %hd not supported\n", event->context.char_uuid.uuid, event->len);
			return;
	}

	switch(event->context.char_uuid.uuid)
	{
		case 0x0000:
			printf("stimulate (%hx): write value %hu\n", event->context.char_uuid.uuid, *((uint8_t*)event->data));
			stimulate_on_gatts_write_stimulate();
			break;

		case 0x0001:
		case 0x0002:
		case 0x0003:
			printf("stimulate (%hx): write value %lu\n", event->context.char_uuid.uuid, *((uint32_t*)event->data));
			break;

		case 0x0004:
		case 0x0005:
		case 0x0006:
		case 0x0007:
			printf("stimulate (%hx): write value %lu\n", event->context.char_uuid.uuid, *((uint32_t*)event->data));
			stimulate_on_gatts_write_pulse_params();
			break;

		case 0x0008:
			printf("stimulate (%hx): write value %lu\n", event->context.char_uuid.uuid, *((uint32_t*)event->data));
			break;
	}
}

void stimulate_on_ble_event(ble_evt_t* event)
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
			stimulate_on_gatts_write_event(&event->evt.gatts_evt.params.write);
			break;
	}
}

static void stimulate_on_gatts_write_stimulate(void)
{
	// normalize to boolean
	stimulate_value = !!stimulate_value;

	// starting and stopping are not idempotent
	// only call them on state transition
	if(stimulate_value && !stimulate_prev)
		stimulate_start();
	else if(!stimulate_value && stimulate_prev)
		stimulate_stop();
	stimulate_prev = stimulate_value;
}

static void stimulate_on_gatts_write_pulse_params(void)
{
	stimulate_compute_config();
	stimulate_update_config(stimulate_value);
}

static void stimulate_compute_config(void)
{
	// positive pulse
	compare0 = compute_timer_compare(&t_positive_pause_value) + 0;
	if(t_positive_pause_value == 0 && t_positive_pulse_value != 0)	// no pulse without pause
		compare0++;
	compare1 = compute_timer_compare(&t_positive_pulse_value) + compare0;

	// negative pulse
	compare2 = compute_timer_compare(&t_negative_pause_value) + compare1;
	if(t_negative_pause_value == 0 && t_negative_pulse_value != 0)	// no pulse without pause
		compare2++;
	compare3 = compute_timer_compare(&t_negative_pulse_value) + compare2;
}

static void stimulate_update_config(uint8_t timer_running)
{
	if(timer_running)
	{
		// protect against concurrent execution with timer event
		CRITICAL_REGION_ENTER();

		// reconfiguration is delayed until the end of the pulse period
		nrf_timer_shorts_enable(stimulate_timer.p_reg, NRF_TIMER_SHORT_COMPARE3_STOP_MASK);
		nrf_drv_timer_compare_int_enable(&stimulate_timer, NRF_TIMER_CC_CHANNEL3);

		CRITICAL_REGION_EXIT();
	}
	else
	{
		// update pulse configuration
		nrf_drv_timer_compare(&stimulate_timer, NRF_TIMER_CC_CHANNEL0, compare0, false);
		nrf_drv_timer_compare(&stimulate_timer, NRF_TIMER_CC_CHANNEL1, compare1, false);
		nrf_drv_timer_compare(&stimulate_timer, NRF_TIMER_CC_CHANNEL2, compare2, false);
		nrf_drv_timer_extended_compare(&stimulate_timer, NRF_TIMER_CC_CHANNEL3, compare3, NRF_TIMER_SHORT_COMPARE3_CLEAR_MASK, false);

		// pulse pin toggling doesn't work if duration is 0, replace with task disabling
		if(compare0 != compare1)
			nrf_drv_gpiote_out_task_enable((nrf_drv_gpiote_pin_t)positive_pin);
		else
			nrf_drv_gpiote_out_task_disable((nrf_drv_gpiote_pin_t)positive_pin);
		if(compare2 != compare3)
			nrf_drv_gpiote_out_task_enable((nrf_drv_gpiote_pin_t)negative_pin);
		else
			nrf_drv_gpiote_out_task_disable((nrf_drv_gpiote_pin_t)negative_pin);
	}
}

static void stimulate_start(void)
{
	// make sure H-bridge is disabled
	nrf_drv_gpiote_out_clear((nrf_drv_gpiote_pin_t)positive_pin);
	nrf_drv_gpiote_out_clear((nrf_drv_gpiote_pin_t)negative_pin);

	// enable stimulation voltage source
	NRF_GPIO->OUTSET = (1 << enable_pin);

	stimulate_compute_config();
	stimulate_update_config(false);
	nrf_drv_timer_enable(&stimulate_timer);
}

static void stimulate_stop(void)
{
	nrf_drv_timer_disable(&stimulate_timer);

	// disable stimulation voltage source
	NRF_GPIO->OUTCLR = (1 << enable_pin);

	// make sure H-bridge is disabled
	nrf_drv_gpiote_out_task_disable((nrf_drv_gpiote_pin_t)positive_pin);
	nrf_drv_gpiote_out_task_disable((nrf_drv_gpiote_pin_t)negative_pin);
}

static void stimulate_timer_event(nrf_timer_event_t event_type, void* p_context)
{
	switch(event_type)
	{
		case NRF_TIMER_EVENT_COMPARE3:
			// protect against concurrent execution with reconfiguration request
			CRITICAL_REGION_ENTER();

			// timer is no longer running at this point
			// update config and re-enable
			stimulate_update_config(false);
			nrf_drv_timer_enable(&stimulate_timer);

			CRITICAL_REGION_EXIT();
			break;

		default:
			break;
	}
}
