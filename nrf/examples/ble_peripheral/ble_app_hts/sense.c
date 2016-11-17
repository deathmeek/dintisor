/*
 * sense.c
 *
 *  Created on: Feb 8, 2016
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


static const ble_uuid128_t full_uuid = {{0x85, 0xf5, 0x2b, 0x3f, 0x03, 0x1b, 0x7d, 0x82, 0xab, 0x49, 0x9e, 0x7f, 0x2d, 0x92, 0x1e, 0x13}};
ble_uuid_t service_uuid;
static ble_uuid_t sense_uuid;

static uint16_t service_handle;
static ble_gatts_char_handles_t sense_handle;

static uint8_t sense_value = 0;

static uint16_t conn_handle = BLE_CONN_HANDLE_INVALID;


void sense_service_init(void)
{
	uint32_t err_code;

	// add service UUID
	service_uuid.uuid = full_uuid.uuid128[13] << 8 | full_uuid.uuid128[12];
	err_code = sd_ble_uuid_vs_add(&full_uuid, &service_uuid.type);
	APP_ERROR_CHECK(err_code);

	// add sensing characteristic UUID
	sense_uuid.uuid = 0x0000;
	sense_uuid.type = service_uuid.type;

	// add service
	err_code = sd_ble_gatts_service_add(BLE_GATTS_SRVC_TYPE_PRIMARY, &service_uuid, &service_handle);
	APP_ERROR_CHECK(err_code);

	// add sensing characteristic
	ble_gatts_attr_md_t sense_attr_md;
	ble_gatts_char_md_t sense_char_md;
	ble_gatts_attr_t sense_attr;

	// characteristic metadata
	memset(&sense_char_md, 0, sizeof(sense_char_md));
	sense_char_md.char_props.read	= 1;
	sense_char_md.char_props.notify	= 1;
	sense_char_md.p_char_user_desc	= NULL;
	sense_char_md.p_char_pf			= NULL;
	sense_char_md.p_user_desc_md	= NULL;
	sense_char_md.p_cccd_md			= NULL;
	sense_char_md.p_sccd_md			= NULL;

	// attribute metadata
	memset(&sense_attr_md, 0, sizeof(sense_attr_md));
	BLE_GAP_CONN_SEC_MODE_SET_OPEN(&sense_attr_md.read_perm);
	BLE_GAP_CONN_SEC_MODE_SET_OPEN(&sense_attr_md.write_perm);
	sense_attr_md.vloc = BLE_GATTS_VLOC_USER;

	// attribute
	memset(&sense_attr, 0, sizeof(sense_attr));
	sense_attr.p_uuid				= &sense_uuid;
	sense_attr.p_attr_md			= &sense_attr_md;
	sense_attr.init_len				= sizeof(sense_value);
	sense_attr.init_offs			= 0;
	sense_attr.max_len				= sense_attr.init_len;
	sense_attr.p_value				= &sense_value;

	err_code = sd_ble_gatts_characteristic_add(service_handle, &sense_char_md, &sense_attr, &sense_handle);
	APP_ERROR_CHECK(err_code);
}

void sense_on_ble_event(ble_evt_t* event)
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

/**
 * @brief Configure ADC for sensor measurement and start conversion.
 */
void sense_measurement_start(void)
{
	NRF_ADC->CONFIG	=	(ADC_CONFIG_RES_8bit << ADC_CONFIG_RES_Pos) |
						(ADC_CONFIG_INPSEL_AnalogInputOneThirdPrescaling << ADC_CONFIG_INPSEL_Pos) |
						(ADC_CONFIG_REFSEL_VBG << ADC_CONFIG_REFSEL_Pos) |
						(ADC_CONFIG_PSEL_AnalogInput2 << ADC_CONFIG_PSEL_Pos) |
						(ADC_CONFIG_EXTREFSEL_None << ADC_CONFIG_EXTREFSEL_Pos);

	// ADC needs high freq clock?
	sd_clock_hfclk_request();

	uint32_t is_running = 0;
	while(!is_running)
		sd_clock_hfclk_is_running(&is_running);

	NRF_ADC->TASKS_START = 1;
}

/**
 * @brief Get sensor value and update characteristics.
 */
void sense_measurement_finish(void)
{
	sense_value = NRF_ADC->RESULT;

	if(conn_handle != BLE_CONN_HANDLE_INVALID)
	{
		uint16_t len = sizeof(sense_value);

		ble_gatts_hvx_params_t params;
		memset(&params, 0, sizeof(params));
		params.type = BLE_GATT_HVX_NOTIFICATION;
		params.handle = sense_handle.value_handle;
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

	// use the STOP task to save energy; workaround for PAN_028 rev1.5 anomaly 1?
	NRF_ADC->TASKS_STOP = 1;

	sd_clock_hfclk_release();
}
