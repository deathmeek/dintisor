/*
 * battery.cpp
 *
 *  Created on: Feb 4, 2016
 *      Author: dan
 */

#include <app_error.h>
#include <ble_bas.h>

#include <nrf.h>
#ifdef NRF51
#include <nrf_drv_adc.h>
#endif /* NRF51 */
#ifdef NRF52
#include <nrf_drv_saadc.h>
#endif /* NRF52 */
#include <nrf_soc.h>

#include <stdint.h>
#include <string.h>


static ble_bas_t service;		// battery service handle

float battery_voltage = 0.0f;


/**
 * @brief Initialize BLE Battery Service.
 */
void battery_service_init(void)
{
	ble_bas_init_t settings;

	// initialize settings
	memset(&settings, 0, sizeof(settings));

	// set security levels
	BLE_GAP_CONN_SEC_MODE_SET_OPEN(&settings.battery_level_char_attr_md.cccd_write_perm);
	BLE_GAP_CONN_SEC_MODE_SET_OPEN(&settings.battery_level_char_attr_md.read_perm);
	BLE_GAP_CONN_SEC_MODE_SET_NO_ACCESS(&settings.battery_level_char_attr_md.write_perm);
	BLE_GAP_CONN_SEC_MODE_SET_OPEN(&settings.battery_level_report_read_perm);

	settings.evt_handler			= NULL;
	settings.support_notification	= true;
	settings.p_report_ref			= NULL;
	settings.initial_batt_level		= 100;

	uint32_t err_code = ble_bas_init(&service, &settings);
	APP_ERROR_CHECK(err_code);
}

/**
 * @brief Process BLE events for Battery Service.
 */
void battery_service_process_event(ble_evt_t* event)
{
	ble_bas_on_ble_evt(&service, event);
}

/**
 * @brief Configure hardware for battery measurement.
 */
uint8_t battery_measurement_init(uint8_t adc_channel)
{
#ifdef NRF51
	static nrf_drv_adc_channel_t channel = {
			.config.config = {
				.resolution = NRF_ADC_CONFIG_RES_10BIT,
				.input = NRF_ADC_CONFIG_SCALING_SUPPLY_ONE_THIRD,
				.reference = NRF_ADC_CONFIG_REF_VBG,	// 1.2V
				.ain = NRF_ADC_CONFIG_INPUT_DISABLED,
			},
	};
	nrf_drv_adc_channel_enable(&channel);
	adc_channel++;
#endif /* NRF51 */

#ifdef NRF52
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
	nrf_drv_saadc_channel_init(adc_channel++, &channel);
#endif /* NRF52 */

	return adc_channel;
}

void battery_measurement_prep()
{
	// no action required
}

/**
 * @brief Get VDD voltage and update battery level.
 */
uint8_t battery_measurement_sample(int16_t* sample)
{
	uint8_t new_level;

#ifdef NRF51
	battery_voltage = *sample * (1.2f / 1024 / (1.0f/3));	// REF=1.2V, RES=10bit, GAIN=1/3
	new_level = battery_voltage * (100 / 3.3f);				// MAX=3.3V
#endif /* NRF51 */

#ifdef NRF52
	battery_voltage = *sample * (0.6f / 1024 / (1.0f/6));	// REF=0.6V, RES=10bit, GAIN=1/6
	new_level = battery_voltage * (100 / 3.3f);				// MAX=3.3V
#endif /* NRF52 */

	uint32_t err_code = ble_bas_battery_level_update(&service, new_level);
	if(	(err_code != NRF_SUCCESS) &&
		(err_code != NRF_ERROR_INVALID_STATE) &&
		(err_code != BLE_ERROR_NO_TX_PACKETS) &&
		(err_code != BLE_ERROR_GATTS_SYS_ATTR_MISSING))
	{
		APP_ERROR_HANDLER(err_code);
	}

	return 1;
}
