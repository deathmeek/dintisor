/*
 * stimulate.c
 *
 *  Created on: Apr 18, 2016
 *      Author: dan
 */

#include <app_error.h>
#include <ble.h>
#include <ble_gap.h>
#include <ble_gatts.h>

#include <nrf.h>
#include <nrf_soc.h>

#include <stddef.h>
#include <stdint.h>
#include <string.h>

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
static uint32_t t_total_value = 0;
static uint32_t t_pulse_value = 0;
static uint32_t t_pause_value = 0;
static uint32_t t_positive_pulse_value = 0;
static uint32_t t_positive_pause_value = 0;
static uint32_t t_negative_pulse_value = 0;
static uint32_t t_negative_pause_value = 0;
static uint32_t amplitude_value = 0;

static uint16_t conn_handle = BLE_CONN_HANDLE_INVALID;


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
	}
}
