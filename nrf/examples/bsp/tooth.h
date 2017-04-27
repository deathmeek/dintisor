/**
 * @author Dan Dragomir <dan.dragomir@cs.pub.ro>
 */
#ifndef TOOTH_H
#define TOOTH_H

#ifdef __cplusplus
extern "C" {
#endif

// LEDs definitions for Tooth
#define LEDS_NUMBER    0

#define LEDS_MASK      (0)
//defines which LEDs are lit when signal is low
#define LEDS_INV_MASK  LEDS_MASK

#define BUTTONS_NUMBER 0

#define BUTTONS_MASK   (0)

// Low frequency clock source to be used by the SoftDevice
#ifdef S210
#define NRF_CLOCK_LFCLKSRC      NRF_CLOCK_LFCLKSRC_RC_250_PPM_TEMP_4000MS_CALIBRATION
#else
#define NRF_CLOCK_LFCLKSRC      {.source        = NRF_CLOCK_LF_SRC_RC,              \
  	                             .rc_ctiv       = 16,                               \
  	                             .rc_temp_ctiv  = 2}
#endif


#ifdef __cplusplus
}
#endif

#endif // TOOTH
