/**
 * @author Dan Dragomir <dan.dragomir@cs.pub.ro>
 */
#ifndef SPARROW_BLE_H
#define SPARROW_BLE_H

// LEDs definitions for Sparrow BLE
#define LEDS_NUMBER    3

#define LED_RGB_RED    16
#define LED_RGB_GREEN  23
#define LED_RGB_BLUE   18

#define LED_RGB_RED_MASK    (1<<LED_RGB_RED)
#define LED_RGB_GREEN_MASK  (1<<LED_RGB_GREEN)
#define LED_RGB_BLUE_MASK   (1<<LED_RGB_BLUE)

#define LEDS_LIST { LED_RGB_RED, LED_RGB_GREEN, LED_RGB_BLUE}
// defining RGB led as 3 single LEDs
#define BSP_LED_0 LED_RGB_RED
#define BSP_LED_1 LED_RGB_GREEN
#define BSP_LED_2 LED_RGB_BLUE

#define BSP_LED_0_MASK    (1<<BSP_LED_0)
#define BSP_LED_1_MASK    (1<<BSP_LED_1)
#define BSP_LED_2_MASK    (1<<BSP_LED_2)

#define LEDS_MASK      (BSP_LED_0_MASK | BSP_LED_1_MASK | BSP_LED_2_MASK)
//defines which LEDs are lit when signal is low
#define LEDS_INV_MASK  LEDS_MASK

#define BUTTONS_NUMBER 2

#define BUTTON_1       19
#define BUTTON_2       20
#define BUTTON_PULL    NRF_GPIO_PIN_PULLUP

#define BUTTONS_LIST { BUTTON_1, BUTTON_2 }

#define BSP_BUTTON_0   BUTTON_1
#define BSP_BUTTON_1   BUTTON_2

#define BSP_BUTTON_0_MASK (1<<BSP_BUTTON_0)
#define BSP_BUTTON_1_MASK (1<<BSP_BUTTON_1)

#define BUTTONS_MASK   (BSP_BUTTON_0_MASK | BSP_BUTTON_1_MASK)

// UART connection with J-Link
#define RX_PIN_NUMBER  9
#define TX_PIN_NUMBER  11
#define HWFC           false

#endif
