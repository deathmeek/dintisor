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
#ifdef NRF51
#include <nrf_drv_adc.h>
#endif /* NRF51 */
#ifdef NRF52
#include <nrf_drv_saadc.h>
#endif /* NRF52 */
#include <nrf_soc.h>

#include <stddef.h>
#include <stdint.h>
#include <string.h>


extern float battery_voltage;


static const ble_uuid128_t		full_uuid = {{0x85, 0xf5, 0x2b, 0x3f, 0x03, 0x1b, 0x7d, 0x82, 0xab, 0x49, 0x9e, 0x7f, 0x2d, 0x92, 0x1e, 0x13}};

static ble_uuid_t				service_uuid;
static uint16_t					service_handle;

static ble_uuid_t				humidity_uuid;
static ble_gatts_char_handles_t	humidity_handle;
static float 					humidity_value = 0.0f;

static ble_uuid_t 				pH_uuid;
static ble_gatts_char_handles_t	pH_handle;
static float 					pH_value = 0.0f;

static uint16_t					conn_handle = BLE_CONN_HANDLE_INVALID;


void sense_service_init(void)
{
	uint32_t err_code;

	// add service UUID
	service_uuid.uuid = full_uuid.uuid128[13] << 8 | full_uuid.uuid128[12];
	err_code = sd_ble_uuid_vs_add(&full_uuid, &service_uuid.type);
	APP_ERROR_CHECK(err_code);

	// add humidity characteristic UUID
	humidity_uuid.uuid				= 0x0002;
	humidity_uuid.type				= service_uuid.type;

	// add pH characteristic UUID
	pH_uuid.uuid					= 0x0003;
	pH_uuid.type					= service_uuid.type;

	// add service
	err_code = sd_ble_gatts_service_add(BLE_GATTS_SRVC_TYPE_PRIMARY, &service_uuid, &service_handle);
	APP_ERROR_CHECK(err_code);

	// common sensing characteristic metadata
	ble_gatts_char_md_t sense_char_md;
	memset(&sense_char_md, 0, sizeof(sense_char_md));
	sense_char_md.char_props.read	= 1;
	sense_char_md.char_props.notify	= 1;
	sense_char_md.p_char_user_desc	= NULL;
	sense_char_md.p_char_pf			= NULL;
	sense_char_md.p_user_desc_md	= NULL;
	sense_char_md.p_cccd_md			= NULL;
	sense_char_md.p_sccd_md			= NULL;

	// common sensing attribute metadata
	ble_gatts_attr_md_t sense_attr_md;
	memset(&sense_attr_md, 0, sizeof(sense_attr_md));
	BLE_GAP_CONN_SEC_MODE_SET_OPEN(&sense_attr_md.read_perm);
	BLE_GAP_CONN_SEC_MODE_SET_OPEN(&sense_attr_md.write_perm);
	sense_attr_md.vloc = BLE_GATTS_VLOC_USER;

	// common sensing attribute
	ble_gatts_attr_t sense_attr;
	memset(&sense_attr, 0, sizeof(sense_attr));
	sense_attr.p_attr_md			= &sense_attr_md;
	sense_attr.init_offs			= 0;

	// add humidity characteristic
	sense_attr.p_uuid				= &humidity_uuid;
	sense_attr.init_len				= sizeof(humidity_value);
	sense_attr.max_len				= sense_attr.init_len;
	sense_attr.p_value				= (void*)&humidity_value;
	err_code = sd_ble_gatts_characteristic_add(service_handle, &sense_char_md, &sense_attr, &humidity_handle);
	APP_ERROR_CHECK(err_code);

	// add pH characteristic
	sense_attr.p_uuid				= &pH_uuid;
	sense_attr.init_len				= sizeof(pH_value);
	sense_attr.max_len				= sense_attr.init_len;
	sense_attr.p_value				= (void*)&pH_value;
	err_code = sd_ble_gatts_characteristic_add(service_handle, &sense_char_md, &sense_attr, &pH_handle);
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
 * @brief Configure hardware for sensor measurement.
 */
uint8_t sense_measurement_init(uint8_t adc_channel)
{
#ifdef NRF52
	ret_code_t err_code;
#endif /* NRF52 */

	// setup humidity channel
#ifdef NRF51
	static nrf_drv_adc_channel_t humidity_channel = {
		.config.config = {
			.resolution = NRF_ADC_CONFIG_RES_10BIT,
			.input = NRF_ADC_CONFIG_SCALING_INPUT_ONE_THIRD,
			.reference = NRF_ADC_CONFIG_REF_VBG,	// 1.2V
			.ain = NRF_ADC_CONFIG_INPUT_2,
		},
	};
	nrf_drv_adc_channel_enable(&humidity_channel);
	adc_channel++;
#endif /* NRF51 */

#ifdef NRF52
	nrf_saadc_channel_config_t humidity_channel = {
		.resistor_p = NRF_SAADC_RESISTOR_DISABLED,
		.resistor_n = NRF_SAADC_RESISTOR_DISABLED,
		.gain = NRF_SAADC_GAIN1_6,
		.reference = NRF_SAADC_REFERENCE_INTERNAL,	// 0.6V
		.acq_time = NRF_SAADC_ACQTIME_10US,
		.mode = NRF_SAADC_MODE_SINGLE_ENDED,
		.burst = NRF_SAADC_BURST_DISABLED,
		.pin_p = NRF_SAADC_INPUT_AIN3,
		.pin_n = NRF_SAADC_INPUT_DISABLED,
	};
	err_code = nrf_drv_saadc_channel_init(adc_channel++, &humidity_channel);
	APP_ERROR_CHECK(err_code);
#endif /* NRF52 */

	// setup pH channel
#ifdef NRF51
	static nrf_drv_adc_channel_t pH_channel = {
		.config.config = {
			.resolution = NRF_ADC_CONFIG_RES_10BIT,
			.input = NRF_ADC_CONFIG_SCALING_INPUT_ONE_THIRD,
			.reference = NRF_ADC_CONFIG_REF_VBG,	// 1.2V
			.ain = NRF_ADC_CONFIG_INPUT_2,
		},
	};
	nrf_drv_adc_channel_enable(&pH_channel);
	adc_channel++;
#endif /* NRF51 */

#ifdef NRF52
	nrf_saadc_channel_config_t pH_channel = {
		.resistor_p = NRF_SAADC_RESISTOR_DISABLED,
		.resistor_n = NRF_SAADC_RESISTOR_DISABLED,
		.gain = NRF_SAADC_GAIN1_6,
		.reference = NRF_SAADC_REFERENCE_INTERNAL,	// 0.6V
		.acq_time = NRF_SAADC_ACQTIME_10US,
		.mode = NRF_SAADC_MODE_SINGLE_ENDED,
		.burst = NRF_SAADC_BURST_DISABLED,
		.pin_p = NRF_SAADC_INPUT_AIN6,
		.pin_n = NRF_SAADC_INPUT_DISABLED,
	};
	err_code = nrf_drv_saadc_channel_init(adc_channel++, &pH_channel);
	APP_ERROR_CHECK(err_code);
#endif /* NRF52 */

	return adc_channel;
}

/**
 * @brief Get sensor value and update characteristics.
 */
uint8_t sense_measurement_sample(int16_t* sample)
{
#ifdef NRF51
	humidity_value	= *sample++ * (1.2f / 1024 / (1.0f/3) / (100.0f / (100.0f + 22.0f)));	// REF=1.2V, RES=10bit, GAIN=1/3, VDD_RES=22K, GND_RES=100K
	pH_value		= *sample++ * (1.2f / 1024 / (1.0f/3) / (100.0f / (100.0f + 22.0f)));	// REF=1.2V, RES=10bit, GAIN=1/3, VDD_RES=22K, GND_RES=100K
#endif /* NRF51 */

#ifdef NRF52
	float humidity_voltage	= *sample++ * (0.6f / 1024 / (1.0f/6));	// REF=0.6V, RES=10bit, GAIN=1/6
	humidity_value			= humidity_voltage * 33.0f / (battery_voltage - humidity_voltage);	// VDD_RES=33K

	float pH_voltage		= *sample++ * (0.6f / 1024 / (1.0f/6));	// REF=0.6V, RES=10bit, GAIN=1/6
	pH_value				= pH_voltage * 33.0f / (battery_voltage - pH_voltage);				// VDD_RES=33K
#endif /* NRF52 */

	if(conn_handle != BLE_CONN_HANDLE_INVALID)
	{
		uint32_t err_code;

		// common update parameters
		uint16_t len;
		ble_gatts_hvx_params_t params;
		memset(&params, 0, sizeof(params));
		params.type = BLE_GATT_HVX_NOTIFICATION;
		params.p_data = NULL;
		params.p_len = &len;

		// update humidity value
		len = sizeof(humidity_value);
		params.handle = humidity_handle.value_handle;
		err_code = sd_ble_gatts_hvx(conn_handle, &params);
		if(	(err_code != NRF_SUCCESS) &&
			(err_code != NRF_ERROR_BUSY) &&
			(err_code != NRF_ERROR_INVALID_STATE) &&
			(err_code != BLE_ERROR_NO_TX_PACKETS) &&
			(err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING))
		{
			APP_ERROR_HANDLER(err_code);
		}

		// update pH value
		len = sizeof(pH_value);
		params.handle = pH_handle.value_handle;
		err_code = sd_ble_gatts_hvx(conn_handle, &params);
		if(	(err_code != NRF_SUCCESS) &&
			(err_code != NRF_ERROR_BUSY) &&
			(err_code != NRF_ERROR_INVALID_STATE) &&
			(err_code != BLE_ERROR_NO_TX_PACKETS) &&
			(err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING))
		{
			APP_ERROR_HANDLER(err_code);
		}
	}

	return 2;
}
