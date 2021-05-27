package de.benvorth.pushr.basicService;

import de.benvorth.pushr.PushrApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path="api")
public class PushrController {

    @RequestMapping(
        path = "/is_alive",
        method = RequestMethod.GET
    )
    public String isAlive() {
        return "alive";
    }

    @RequestMapping(
        path = "/push",
        method = RequestMethod.GET
    )
    public String push(
        @RequestParam(name="token", required = true) String token,
        @RequestParam(name="bat", required = false) String bat, // Batteriespannung
        @RequestParam(name="per", required = false) String per, // Batteriekapazität in %
        @RequestParam(name="mac", required = false) String mac, // MAC Adresse des Buttons (WiFi)
        @RequestParam(name="bssid", required = false) String bssid, // MAC Adresse des WiFi Access Points
        @RequestParam(name="ssid", required = false) String ssid, // SSID des WiFi Access Points
        @RequestParam(name="psk", required = false) String psk, // Pass Key des WiFi Access Points
        @RequestParam(name="blm", required = false) String blm, // MAC Adresse des stärksten iBeacons
        @RequestParam(name="blu", required = false) String blu, // UUID Kennung des stärksten iBeacons
        @RequestParam(name="blv", required = false) String blv, // Batterie Spannung des stärksten iBeacons (TLM Paket aktiv)
        @RequestParam(name="cpu", required = false) String cpu, // Counter Push
        @RequestParam(name="cap", required = false) String cap, //  Counter AP
        @RequestParam(name="cst", required = false) String cst, // Counter STA
        @RequestParam(name="cwp", required = false) String cwp, // Counter WPS
        @RequestParam(name="swver", required = false) String swver, // SW-version
        @RequestParam(name="hwver", required = false) String hwver // HW-version
    ) {
        PushrApplication.logger.info(
            "++++received push: \n" +
            "    token {}\n" +
            "    Batteriespannung {}\n" +
            "    Batteriekapazität in % {}\n" +
            "    MAC Adresse des Buttons (WiFi) {}\n" +
            "    MAC Adresse des WiFi Access Points {}\n" +
            "    SSID des WiFi Access Points {}\n" +
            "    Pass Key des WiFi Access Points {}\n" +
            "    MAC Adresse des stärksten iBeacons {}\n" +
            "    UUID Kennung des stärksten iBeacons {}\n" +
            "    Batterie Spannung des stärksten iBeacons {}\n" +
            "    Counter Push {}\n" +
            "    Counter AP {}\n" +
            "    Counter STA {}\n" +
            "    Counter WPS {}\n" +
            "    SW-version {}\n" +
            "    HW-version {}",

            token,
            (bat != null ? bat : "null"),
            (per != null ? per : "null"),
            (mac != null ? mac : "null"),
            (bssid != null ? bssid : "null"),
            (ssid != null ? ssid : "null"),
            (psk != null ? psk : "null"),
            (blm != null ? blm : "null"),
            (blu != null ? blu : "null"),
            (blv != null ? blv : "null"),
            (cpu != null ? cpu : "null"),
            (cap != null ? cap : "null"),
            (cst != null ? cst : "null"),
            (cwp != null ? cwp : "null"),
            (swver != null ? swver : "null"),
            (hwver != null ? hwver : "null")
        );
        return "alive";
    }
}
