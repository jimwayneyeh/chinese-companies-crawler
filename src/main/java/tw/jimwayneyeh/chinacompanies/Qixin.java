package tw.jimwayneyeh.chinacompanies;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import tw.jimwayneyeh.chinacompanies.resources.Gson;

public class Qixin {
  private static final Logger LOGGER = LoggerFactory.getLogger(Qixin.class);
  private WebClient client;
  
  public Qixin (WebClient client) {
    client.getOptions().setJavaScriptEnabled(true);
    client.getOptions().setCssEnabled(true);
    client.getOptions().setThrowExceptionOnScriptError(false);
    client.getOptions().setTimeout(30000);
    
    this.client = client;
  }
  
  public void get (UUID companyId) throws IOException {
    URL url = new URL(
        "https://www.qixin.com/company-operation/" + companyId.toString());
    
    HtmlPage rootPage = this.client.getPage(url);
    LOGGER.trace("A company '{}' is queried...", companyId);
    
    getCompanyOperation(rootPage);
  }
  
  protected JsonElement getCompanyOperation (HtmlPage rootPage) {
    LOGGER.debug("Attempt to find out the basic information.");
    String bodyStr = rootPage.getWebResponse().getContentAsString();
    
    Document bodyDoc = Jsoup.parse(bodyStr);
    
    Element lastScript = bodyDoc.getElementsByTag("script").last();
    String scriptHtml = lastScript.html();
    
    int beginIndex = scriptHtml.indexOf(".concat(") + 8;
    int endIndex = scriptHtml.lastIndexOf(")||w");
    
    scriptHtml = scriptHtml.substring(beginIndex, endIndex);
    
    
    
    // Parse the JSON.
    JsonObject sourceJson = 
        Gson.JSON_PARSER.parse(scriptHtml).getAsJsonObject();
    LOGGER.trace("Source: {}", sourceJson);
    JsonArray wArray = sourceJson
        .get("o").getAsJsonObject()
        .get("w").getAsJsonArray();
    
    for (JsonElement element : wArray) {
      for (JsonElement subElement : element.getAsJsonArray()) {
        if (!subElement.isJsonObject()) {
          continue;
        }
        
        JsonObject json = subElement.getAsJsonObject();
        if (json.has("name")) {
          LOGGER.trace("Company: {}", json.get("name"));
          LOGGER.trace("Details: {}", json);
          return json;
        }
      }
    }
    
    return JsonNull.INSTANCE;
  }
}