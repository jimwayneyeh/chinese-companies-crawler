package tw.jimwayneyeh.chinacompanies;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public class Qichacha {
  private static final Logger LOGGER = LoggerFactory.getLogger(Qichacha.class);
  private static final SecureRandom RANDOM = new SecureRandom();
  
  public static final String TAB_RUN = "run";
  
  private WebClient client;
  protected String entranceUrl;
  protected String companyId;
  protected String companyName;
  
  public Qichacha (WebClient client) {
    client.getOptions().setJavaScriptEnabled(true);
    client.getOptions().setRedirectEnabled(true);
    client.getOptions().setCssEnabled(true);
    client.getOptions().setThrowExceptionOnScriptError(false);
    client.getOptions().setTimeout(30000);
    
    this.client = client;
  }
  
  public void get (String companyId) throws IOException {
    try {
      this.companyId = companyId;
      getContent();
    } catch (InterruptedException ite) {
      LOGGER.warn("The thread is interrupted.", ite);
    }
  }
  
  protected void getContent () 
      throws IOException, InterruptedException {
    // ade01c554b262c9550b4a7ffeaaac80e
    entranceUrl = 
        String.format("https://www.qichacha.com/firm_%s.html", companyId);
    LOGGER.info("Attempt to query '{}'.", entranceUrl);
    
    WebRequest request = new WebRequest(new URL(entranceUrl));
    request.setAdditionalHeader("Cookie", System.getenv("QICHACHA_COOKIE"));
    request.setAdditionalHeader("Referer", entranceUrl);
    HtmlPage page = this.client.getPage(request);
    
    companyName =
        getCompanyName(page.getWebResponse().getContentAsString());
    LOGGER.info("Company: {}", companyName);
    
    JsonObject result = new JsonObject();
    
    /*
     * Crawl and get the details.
     */
    LOGGER.info("Attempt to get the basic information of the company.");
    Thread.sleep(2000 + RANDOM.nextInt(3000));
    page = getCompanyInfo("base");
    result = parseBasicInformation(
        page.getWebResponse().getContentAsString(), result);
    
    LOGGER.info("Attempt to get the judicals of the company.");
    Thread.sleep(2000 + RANDOM.nextInt(3000));
    page = getCompanyInfo("susong");
    result = parseJudicature(
        page.getWebResponse().getContentAsString(), result);
    
    LOGGER.info("Attempt to get the operation of the company.");
    Thread.sleep(2000 + RANDOM.nextInt(3000));
    page = getCompanyInfo("run");
    result = parseCompanyOperation(
        page.getWebResponse().getContentAsString(), result);
    
    LOGGER.info("Attempt to get the IPs of the company.");
    Thread.sleep(2000 + RANDOM.nextInt(3000));
    page = getCompanyInfo("assets");
    result = parseIntellectualProperties(
        page.getWebResponse().getContentAsString(), result);
    
    LOGGER.info("Attempt to get the reports of the company.");
    Thread.sleep(2000 + RANDOM.nextInt(3000));
    page = getCompanyInfo("report");
    result = parseReports(
        page.getWebResponse().getContentAsString(), result);
    
    LOGGER.info("Overall result: {}", result);
  }
  
  protected String getCompanyName (String html) {
    Elements contents = Jsoup.parse(html)
        .getElementsByAttributeValue("class", "content");
    return contents.first().child(0).ownText().trim();
  }
  
  protected HtmlPage getCompanyInfo (String tab) 
          throws IOException {
    return getCompanyInfo(tab, StringUtils.EMPTY, 0);
  }
  
  protected HtmlPage getCompanyInfo (String tab, String box, int page)
      throws IOException {
    
    URL url;
    try {
      URIBuilder builder = new URIBuilder(
              "http://www.qichacha.com/company_getinfos")
          .addParameter("unique", companyId)
          .addParameter("companyname", companyName)
          .addParameter("tab", tab);
      if (StringUtils.isNotBlank(box)) {
        builder.addParameter("box", box);
        builder.addParameter("p", String.valueOf(page));
      }
      
      url = builder.build().toURL();
      LOGGER.trace("URL to be queried: {}", url);
    } catch (URISyntaxException use) {
      LOGGER.error("Cannot build the URL.", use);
      return null;
    }
    
    // Sleep for a while to ensure that it will not be banned.
    try {
      Thread.sleep(300 + RANDOM.nextInt(700));
    } catch (InterruptedException e) {}
    
    WebRequest request = new WebRequest(url);
    request.setAdditionalHeader("Cookie", System.getenv("QICHACHA_COOKIE"));
    request.setAdditionalHeader("Referer", entranceUrl);
    return this.client.getPage(request);
  }
  
  protected JsonObject parseBasicInformation (
      String html, JsonObject result) {
    LOGGER.info("Attempt to parse the basic information.");
    
    Document doc = Jsoup.parse(html);
    
    // 工商信息 Cominfo
    addKeyValueTableToJson(doc.getElementById("Cominfo"), result, "工商信息");
    
    // 股东信息 Sockinfo
    addTableToJson(doc.getElementById("Sockinfo"), result, "股东信息");
    
    // 主要人员 Mainmember
    addTableToJson(doc.getElementById("Mainmember"), result, "主要人员");
    
    // 分支机构 Subcom
    addKeyValueTableToJson(doc.getElementById("Subcom"), result, "分支机构");
    
    // 历史法定代表人 historyData2
    addTableToJson(doc.getElementById("historyData2"), result, "历史法定代表人");
    
    // TODO 历史股东
    result.add("历史股东", JsonNull.INSTANCE);
    
    // 变更记录 Changelist
    addTableToJson(doc.getElementById("Changelist"), result, "变更记录");
    
    // TODO 公司简介
    result.add("公司简介", JsonNull.INSTANCE);
    
    return result;
  }
  
  protected JsonObject parseJudicature (String html, JsonObject result) {
    LOGGER.info("Attempt to parse the judicature.");
    
    Document doc = Jsoup.parse(html);
    
    // 被执行人信息 zhixinglist
    addTableToJson(doc.getElementById("zhixinglist"), result, "被执行人信息");
    
    // TODO 失信被执行人
    result.add("失信被执行人", JsonNull.INSTANCE);
    
    // 开庭公告 noticelist
    addTableToJson(doc.getElementById("noticelist"), result, "开庭公告");
    
    // TODO 司法协助
    result.add("司法协助", JsonNull.INSTANCE);
    
    // 裁判文书 wenshulist
    addTableToJson(doc.getElementById("wenshulist"), result, "裁判文书");
    
    // 法院公告 gonggaolist
    addTableToJson(doc.getElementById("gonggaolist"), result, "法院公告");
    
    return result;
  }
  
  protected JsonObject parseCompanyOperation (
      String html, JsonObject result) {
    LOGGER.info("Attempt to parse the operations.");
    
    Document doc = Jsoup.parse(html);
    
    // TODO 经营异常
    result.add("经营异常", JsonNull.INSTANCE);
    
    // 股权出质 pledgeList
    addTableToJson(doc.getElementById("pledgeList"), result, "股权出质");
    
    // 行政处罚 penaltylist
    addTableToJson(doc.getElementById("penaltylist"), result, "行政处罚");
    
    // 行政许可 permissionlist
    addTableToJson(doc.getElementById("permissionlist"), result, "行政许可");
    
    // TODO 抽查检查
    result.add("抽查检查", JsonNull.INSTANCE);
    
    // 税务信用 taxCreditList
    addTableToJson(doc.getElementById("taxCreditList"), result, "税务信用");
    
    // TODO 税收违法
    result.add("税收违法", JsonNull.INSTANCE);
    
    // 产品信息 productlist
    addTableToJson(doc.getElementById("productlist"), result, "产品信息");
    
    // 融资信息 financingList
    addTableToJson(doc.getElementById("financingList"), result, "融资信息");
    
    // TODO 动产抵押
    result.add("动产抵押", JsonNull.INSTANCE);
    
    // TODO 清算信息
    result.add("清算信息", JsonNull.INSTANCE);
    
    // 招投标信息 tenderlist
    addTableToJson(doc.getElementById("tenderlist"), result, "招投标信息");
    
    // 公司员工 lietoulist
    addTableToJson(doc.getElementById("lietoulist"), result, "公司员工");
    
    // 招聘 joblist
    addTableToJson(doc.getElementById("joblist"), result, "招聘");
    
    // 财务总览 V3_cwzl
    addKeyValueTableToJson(doc.getElementById("V3_cwzl"), result, "财务总览");
    
    // TODO 司法拍卖
    result.add("司法拍卖", JsonNull.INSTANCE);
    
    // 进出口信用信息 ciaxList
    addTableToJson(doc.getElementById("ciaxList"), result, "进出口信用信息");
    
    // 微信公众号 wechatlist
    addTableToJson(doc.getElementById("wechatlist"), result, "微信公众号");
    
    // TODO 简易注销
    result.add("简易注销", JsonNull.INSTANCE);
    
    // TODO 新闻舆情
    result.add("新闻舆情", JsonNull.INSTANCE);
    
    // TODO 公示催告
    result.add("公示催告", JsonNull.INSTANCE);
    
    // TODO 公告研报
    result.add("公告研报", JsonNull.INSTANCE);
    
    return result;
  }
  
  protected JsonObject parseIntellectualProperties (
      String html, JsonObject result) {
    LOGGER.info("Attempt to parse the intellectual properties.");
    
    Document doc = Jsoup.parse(html);
    
    // 商标信息 shangbiaolist
    addTableToJson(doc.getElementById("shangbiaolist"), result, "商标信息");
    
    // 专利信息 zhuanlilist
    addTableToJson(doc.getElementById("zhuanlilist"), result, "专利信息");
    
    // 证书信息 zhengshulist
    addTableToJson(doc.getElementById("zhengshulist"), result, "证书信息");
    
    // 作品著作权 zzqlist
    addTableToJson(doc.getElementById("zzqlist"), result, "作品著作权");
    
    // 软件著作权 rjzzqlist
    addTableToJson(doc.getElementById("rjzzqlist"), result, "软件著作权");
    
    // 网站信息 websitelist
    addTableToJson(doc.getElementById("websitelist"), result, "网站信息");
    
    return result;
  }
  
  protected JsonObject parseInvestments (
      String html, JsonObject result) {
    LOGGER.info("Attempt to parse the investments.");
    
    Document doc = Jsoup.parse(html);
    
    // 对外投资 shangbiaolist
    addTableToJson(doc.getElementById("shangbiaolist"), result, "对外投资");
    
    return result;
  }
  
  protected JsonObject parseReports (String html, JsonObject result) {
    LOGGER.info("Attempt to parse the reports.");
    
    try {
      Document doc = Jsoup.parse(html);
      
      Elements years = doc.getElementById("myTab").getElementsByTag("a");
      
      for (Element yearElement : years) {
        String year = yearElement.text().trim();
        LOGGER.trace("Attempt to find out report for '{}'.", year);
        
        Element reportElement = doc.getElementById(year);
        if (reportElement != null) {
          result.add(year, parseKeyValueTable(reportElement));
        }
      }
    } catch (Throwable t) {
      LOGGER.error("Error when parsing the reports.", t);
    }
    
    return result;
  }
  
  private void addTableToJson (
      Element element, JsonObject result, String key) {
    LOGGER.debug("Trying to extract '{}'...", key);
    JsonElement value = JsonNull.INSTANCE;
    if (element != null) {
      try {
        value = getTableAsJson(element);
      } catch (Throwable t) {
        LOGGER.warn("Error when parsing a table.", t);
      }
    }
    result.add(key, value);
  }
  
  private void addKeyValueTableToJson (
      Element element, JsonObject result, String key) {
    LOGGER.debug("Trying to extract '{}'...", key);
    JsonElement value = JsonNull.INSTANCE;
    if (element != null) {
      try {
        value = parseKeyValueTable(element);
      } catch (Throwable t) {
        LOGGER.warn("Error when parsing a table.", t);
      }
    }
    result.add(key, value);
  }
  
  /**
   * Example: 
   *    javascript:getTabList(2,"run","news")
   */
  private static Pattern TABLE_PAGING = Pattern.compile(
      "\\SgetTabList\\((?<page>[0-9]+),\"(?<tab>[\\S]+)\",\"(?<box>[\\S]+)\"\\)");
  
  private JsonArray getTableAsJson (Element table) {
    JsonArray result = parseTable(table);
    
    Elements paginations = 
        table.getElementsByAttributeValue("class", "pagination");
    if (paginations != null && paginations.size() > 0) {
      Element pageElement = paginations.get(0).getElementsByTag("a").last();
      Matcher matcher = TABLE_PAGING.matcher(pageElement.attr("href").trim());
      if (matcher.find()) {
        int lastPage = Integer.parseInt(matcher.group("page"));
        String tab = matcher.group("tab");
        String box = matcher.group("box");
        
        LOGGER.debug("Find paging: {} {} {}", lastPage, tab, box);
        
        // Traverse the pagination.
        for (int page = 2; page <= lastPage; page++) {
          try {
            HtmlPage nextPage = getCompanyInfo(tab, box, page);
            
            Element tableElement = Jsoup
                .parse(nextPage.getWebResponse().getContentAsString())
                .getElementsByTag("table")
                .get(0);
            
            JsonArray nextPageItems = parseTable(tableElement);
            result.addAll(nextPageItems);
          } catch (IOException e) {
            LOGGER.error("Error when getting paging of a box.", e);
          }
        }
      }
    }
    
    LOGGER.trace("Find {} elements.", result.size());
    return result;
  }
  
  private static JsonArray parseTable (Element table) {
    Elements headers = null;
    
    JsonArray result = new JsonArray();
    
    for (Element tr : table.getElementsByTag("tr")) {
      // Ignore the header of the table.
      Elements ths = tr.getElementsByTag("th");
      if (ths.size() > 0) {
        headers = ths;
        continue;
      }
      
      Elements children = tr.children();
      JsonObject msg = new JsonObject();
      
      for (int i = 0; i < children.size(); i++) {
        msg.addProperty(
            headers.get(i).text().trim(), 
            children.get(i).text().trim());
      }
      
      result.add(msg);
    }
    
    return result;
  }
  
  private static JsonArray parseKeyValueTable (Element table) {
    JsonArray result = new JsonArray();
    
    for (Element tr : table.getElementsByTag("tr")) {
      Elements children = tr.children();
      JsonObject msg = new JsonObject();
      
      if (children.size() % 2 != 0) {
        // Ignore rows which are not composed with key-value.
        continue;
      }
      
      for (int i = 0; i < children.size(); i++) {
        msg.addProperty(
            children.get(i).text().trim(), 
            children.get(++i).ownText().trim());
      }
      
      result.add(msg);
    }
    
    return result;
  }
}