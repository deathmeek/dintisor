/*
 * sense.h
 *
 *  Created on: Feb 8, 2016
 *      Author: dan
 */

#ifndef DINTISOR_SENSE_H_
#define DINTISOR_SENSE_H_

#include <ble.h>
#include <ble_types.h>

#include <stdint.h>


ble_uuid_t service_uuid;


void sense_service_init(void);
void sense_on_ble_event(ble_evt_t* event);

uint8_t sense_measurement_init(uint8_t adc_channel);
uint8_t sense_measurement_sample(int16_t* sample);


#endif /* DINTISOR_SENSE_H_ */
