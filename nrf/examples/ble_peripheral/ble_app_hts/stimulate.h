/*
 * stimulate.h
 *
 *  Created on: Apr 18, 2016
 *      Author: dan
 */

#ifndef DINTISOR_STIMULATE_H_
#define DINTISOR_STIMULATE_H_

#include <ble.h>

#include <stdint.h>


void stimulate_service_init(void);
void stimulate_service_on_ble_event(ble_evt_t* event);

uint8_t stimulate_measurement_init(uint8_t adc_channel);
void stimulate_measurement_sample(int16_t sample);


#endif /* DINTISOR_STIMULATE_H_ */
