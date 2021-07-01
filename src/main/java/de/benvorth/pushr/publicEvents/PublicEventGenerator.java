package de.benvorth.pushr.publicEvents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.benvorth.pushr.model.device.Device;
import de.benvorth.pushr.model.device.DeviceRespository;
import de.benvorth.pushr.model.message.PushMessage;
import de.benvorth.pushr.pushService.PushMsgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class PublicEventGenerator {

    private final RestTemplate httpClient;
    private final ObjectMapper objectMapper;
    private final DeviceRespository deviceRespository;
    private final PushMsgController pushMsgController;


    private int lastXkcdNum = -1;

    @Autowired
    public PublicEventGenerator (
        ObjectMapper objectMapper,
        DeviceRespository deviceRespository,
        PushMsgController pushMsgController
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = new RestTemplate();
        this.deviceRespository = deviceRespository;
        this.pushMsgController = pushMsgController;
    }

    // check every ten mins on a new xkcd comic
    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    public void newXkcdComic() {
        // https://xkcd.com/info.0.json
        // {"month": "6", "num": 2479, "link": "", "year": "2021", "news": "",
        // "safe_title": "Houseguests", "transcript": "",
        // "alt": "You can come on in, we're all fully vaccinated. Except the spare room off the living room. Don't go in there, we're not fully vaccinated in there.",
        // "img": "https://imgs.xkcd.com/comics/houseguests.png", "title": "Houseguests",
        // "day": "21"}

        ResponseEntity<String> response = this.httpClient.getForEntity(
            "https://xkcd.com/info.0.json", String.class);

        if (response.getStatusCodeValue() == 200) {
            Map xkcdJson = null;
            try {
                xkcdJson = this.objectMapper.readValue(response.getBody(), Map.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            int comicNum = (int)(xkcdJson.get("num"));
            if (lastXkcdNum < comicNum) { // a new one!
                lastXkcdNum = comicNum;

                int year = Integer.parseInt((String) xkcdJson.get("year"));
                int month = Integer.parseInt((String) xkcdJson.get("month"));
                int day = Integer.parseInt((String) xkcdJson.get("day"));
                String title = (String) xkcdJson.get("safe_title");
                String alt = (String) xkcdJson.get("alt");
                String imgUrl = (String) xkcdJson.get("img");

                PushMessage pm = new PushMessage();
                pm.setTitle("pushr.info: New xkcd comic!");
                // pm.setImage(imgUrl);
                pm.setImage("https://xkcd.com/s/0b7742.png"); // generic xkcd image
                // pm.setIcon("https://xkcd.com/s/919f27.ico"); // xkcd icon
                pm.setIcon("https://pushr.info/img/pushr-72.png"); // pushr icon
                pm.setBadge("https://xkcd.com/s/919f27.ico");
                pm.setBody("'" + title + "' (No " + comicNum + ")");
                pm.setTimestamp(System.currentTimeMillis());

                for (Device device : deviceRespository.findAll()) {
                    this.pushMsgController.sendTextPushMessage(device, pm);
                }
            }




        }
    }
}
