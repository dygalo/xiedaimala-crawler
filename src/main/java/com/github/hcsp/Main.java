package com.github.hcsp;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Main {
    public static void main(String[] args) throws IOException {
        List<String> linkPool = new ArrayList<>();
        Set<String> processedLinkeds = new HashSet<>();
        linkPool.add("https://sina.cn");
        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }
            String link = linkPool.remove((linkPool.size() - 1));
            if (processedLinkeds.contains(link)) {
                continue;
            }
            if (isWannaLink(link)) {
                CloseableHttpClient httpclient = HttpClients.createDefault();
                if (link.startsWith("https:\\/\\/")) {
                    continue;
                }
                if (link.startsWith("https://passport")){
                    continue;
                }
                System.out.println(link);
//                if (link.startsWith("//")) {
//                    link = "https:" + link;
//                }
                HttpGet httpGet = new HttpGet(link);
                httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36");
                try (CloseableHttpResponse response1 = httpclient.execute(httpGet)) {
                    System.out.println(response1.getStatusLine());
                    Document doc = getAndParseHtml(response1);
                    getPotentialLink(linkPool, doc);
                    ArrayList<Element> articles = doc.select("article");
                    SaveToListIfNews(processedLinkeds, link, articles);

                }
            }
        }
    }

    private static void SaveToListIfNews(Set<String> processedLinkeds, String link, ArrayList<Element> articles) {
        if (!articles.isEmpty()) {
            for (Element article : articles) {
                System.out.println(articles.get(0).child(0).text());
            }
            processedLinkeds.add(link);
        }
    }

    private static void getPotentialLink(List<String> linkPool, Document doc) {
        ArrayList<Element> links = doc.select("a");
        links.stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);
//        for (Element aTag : links) {
//            //通过属性来获得链接，a标签的属性里就是链接
//            linkPool.add(aTag.attr("href"));
//
//        }
    }

    private static Document getAndParseHtml(CloseableHttpResponse response1) throws IOException {
        HttpEntity entity1 = response1.getEntity();
        String html = EntityUtils.toString(entity1);
        return Jsoup.parse(html);
    }

    private static boolean isWannaLink(String link) {
        return (link.contains("news.sina.cn")) || (link.equals("https://sina.cn"));
    }

}
