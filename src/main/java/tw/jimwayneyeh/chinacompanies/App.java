package tw.jimwayneyeh.chinacompanies;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.WebClient;

public class App {
  private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
  
  public static void main(String[] args) {
    LOGGER.info("Initiate the web client.");
    WebClient client = new WebClient();
    
    LOGGER.debug("Query qixin...");
    try {
      Qixin qixin = new Qixin(client);
      qixin.get(UUID.fromString("edcce542-88e9-4912-8b7f-687a3e1ac060"));
      //qixin.get(UUID.fromString("4e2021d8-e552-49bb-ba55-afa76b9f4d4c"));
      
    } catch (Throwable t) {
      LOGGER.error("Error.", t);
    }
  }
}
