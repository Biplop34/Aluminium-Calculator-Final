package com.example

import com.example.model.MeasurementSet
import com.example.util.Base8Math
import com.example.util.SmartNotepadParser
import com.example.util.ThaiFabricationMath
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testBase8MathPartsConversion() {
    val parts = Base8Math.toParts("35.4")
    assertEquals(35 * 8 + 4, parts)
    assertEquals("35.4", Base8Math.fromParts(parts))
  }

  @Test
  fun testBase8MathAddition() {
    // 35 inches 4 suta + 1 inch 6 suta = 36 inches 10 suta = 37 inches 2 suta
    val a = Base8Math.toParts("35.4")
    val b = Base8Math.toParts("1.6")
    val sum = Base8Math.calculate("+", a, b)
    assertEquals("37.2", Base8Math.fromParts(sum))
  }

  @Test
  fun testBase8MathSubtraction() {
    val a = Base8Math.toParts("37.2")
    val b = Base8Math.toParts("1.6")
    val diff = Base8Math.calculate("-", a, b)
    assertEquals("35.4", Base8Math.fromParts(diff))
  }

  @Test
  fun testThaiFabricationCalculation() {
    val set = MeasurementSet(height = "120.0", length1 = "150.0", length2 = "149.5")
    val result = ThaiFabricationMath.calculateItem(set)
    assertNotNull(result)
    assertEquals("120.0", result!!.outerSide)
    assertEquals("148.7", result.outerTop)
    assertEquals("148.2", result.outerBottom)
    assertEquals("117.3", result.lockVal)
  }

  @Test
  fun testSmartNotepadParser() {
    val text = """
      # 206.1 x 181.9
      # 206/1 x 181/9
      # 206-1 x 181-9
      #202*1 x 181*9
      136.7 x 149.7/150
      1. 21/2 x 46 - 45/5
      2. 54.5 x 68.2
    """.trimIndent()
    val parsed = SmartNotepadParser.parse(text)
    assertEquals(7, parsed.size)
    
    // # 206.1 x 181.9
    assertEquals("206.1", parsed[0].height)
    assertEquals("181.9", parsed[0].length1)
    assertEquals("", parsed[0].length2)

    // # 206/1 x 181/9
    assertEquals("206.1", parsed[1].height)
    assertEquals("181.9", parsed[1].length1)
    assertEquals("", parsed[1].length2)

    // # 206-1 x 181-9
    assertEquals("206.1", parsed[2].height)
    assertEquals("181.9", parsed[2].length1)
    assertEquals("", parsed[2].length2)

    // #202*1 x 181*9
    assertEquals("202.1", parsed[3].height)
    assertEquals("181.9", parsed[3].length1)
    assertEquals("", parsed[3].length2)

    // 136.7 x 149.7/150
    assertEquals("136.7", parsed[4].height)
    assertEquals("149.7", parsed[4].length1)
    assertEquals("150", parsed[4].length2)

    // 1. 21/2 x 46 - 45/5
    assertEquals("21.2", parsed[5].height)
    assertEquals("46", parsed[5].length1)
    assertEquals("45.5", parsed[5].length2)

    // 2. 54.5 x 68.2
    assertEquals("54.5", parsed[6].height)
    assertEquals("68.2", parsed[6].length1)
    assertEquals("", parsed[6].length2)
  }

  @Test
  fun testUserNotepadFormatWithSerialSlash() {
    val text = """
      ‎1 / 177.8*165.4
      ‎2/ 178*165.1
      ‎3/ 124.5*102
      ‎4/ 176.9*166/165.6
      ‎5/ 177*166.2
    """.trimIndent()
    val parsed = SmartNotepadParser.parse(text)
    assertEquals(5, parsed.size)

    assertEquals("177.8", parsed[0].height)
    assertEquals("165.4", parsed[0].length1)
    assertEquals("", parsed[0].length2)

    assertEquals("178", parsed[1].height)
    assertEquals("165.1", parsed[1].length1)
    assertEquals("", parsed[1].length2)

    assertEquals("124.5", parsed[2].height)
    assertEquals("102", parsed[2].length1)
    assertEquals("", parsed[2].length2)

    assertEquals("176.9", parsed[3].height)
    assertEquals("166", parsed[3].length1)
    assertEquals("165.6", parsed[3].length2)

    assertEquals("177", parsed[4].height)
    assertEquals("166.2", parsed[4].length1)
    assertEquals("", parsed[4].length2)
  }

  @Test
  fun testHardwareCalculation() {
    val set1 = MeasurementSet(height = "136.0", length1 = "150.0") // Standard/Oversized
    val set2 = MeasurementSet(height = "50.0", length1 = "50.0")   // Fanlight

    val res1 = ThaiFabricationMath.calculateItem(set1)
    val res2 = ThaiFabricationMath.calculateItem(set2)
    assertNotNull(res1)
    assertNotNull(res2)

    val hw = ThaiFabricationMath.calculateHardware(listOf(res1!!, res2!!), "4")
    assertEquals("2 pcs", hw.slidingLock) // 1 non-fanlight * 2
    assertEquals("4 pcs", hw.slidingWheelRegular) // 136 x 150 = 21.96 sq ft <= 22.5 sq ft -> Regular wheel!
    assertEquals("0 pcs", hw.slidingWheelGaton) // 0 pcs
    assertEquals("16 pcs", hw.kotScrews)  // 2 windows * 8
    assertEquals("35 pcs", hw.screws1_5)  // 27 + 8
    assertEquals("19 pcs", hw.royalPlugs) // 35 - 16
    assertEquals("8 pcs", hw.mosquitoNetAngel) // 2 windows * 4 (including fanlight!)
    assertEquals("2 pcs", hw.mosquitoNetHandle) // 2 windows * 1 (including fanlight!)
    assertEquals("2 pcs", hw.mosquitoNetWheel) // 1 non-fanlight * 2
    assertTrue(hw.mohiar.contains("For Fanlight:"))
    assertTrue(hw.items.any { it.isMosquitoNet })
    // Ensure 0-items like Gaton Wheel and Doubt Wheel are auto-hidden from items list
    assertFalse(hw.items.any { it.name == "Sliding Wheel (Gaton)" })
    assertFalse(hw.items.any { it.name == "Sliding Wheel (Doubt Wheel)" })
    assertTrue(hw.items.any { it.name == "Sliding Wheel (Regular)" })
  }
}
