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
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Main {
    private static List<String> loadUrlsFromDatabase(Connection connection, String sql) throws SQLException {
        List<String> results=new ArrayList<>();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                results.add(resultSet.getString(1));
            }
        }
        return results;
    }
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:C:/Users/25224/Desktop/xiedaimala-crawler");
        List<String> linkPool = loadUrlsFromDatabase(connection, "select LINK from LINKS_TO_BE_PROCESSED");
//        Set<String> processedLinkeds =new HashSet<>(loadUrlsFromDatabase(connection,"select LINK from lINKED_ALREADY_PROCESSED"));
        try {
            while (true) {
                if (linkPool.isEmpty()) {
                    break;
                }
                String link = linkPool.remove((linkPool.size() - 1));
                try(PreparedStatement preparedStatement=connection.prepareStatement("DELETE  FROM LINKS_TO_BE_PROCESSED where LINK = ?")){
                preparedStatement.setString(1,link);
                preparedStatement.executeUpdate();}
                boolean flag=false;
                try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT  LINK FROM LINKED_ALREADY_PROCESSED where link = ?")) {
                   preparedStatement.setString(1,link);
                   ResultSet resultSet=preparedStatement.executeQuery();
                    while (resultSet.next()) {
                        flag=true;
                    }
                }

                if (flag) {
                    continue;
                }
                if (isWannaLink(link)) {
                    CloseableHttpClient httpclient = HttpClients.createDefault();
                    if (link.startsWith("https:\\/\\/")) {
                        continue;
                    }
                    if (link.startsWith("https://passport")) {
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
                        if (!articles.isEmpty()) {
                            for (Element article : articles) {
                                System.out.println(articles.get(0).child(0).text());
                            }}}
                            try(PreparedStatement preparedStatement=connection.prepareStatement("INSERT INTO LINKED_ALREADY_PROCESSED (LINK) values ( ? )")){
                                preparedStatement.setString(1,link);
                                preparedStatement.executeUpdate();}


                }
            } }finally{

            }
        }




    private static void getPotentialLink(List<String> linkPool, Document doc) {
        ArrayList<Element> links = doc.select("a");
        links.stream().map(aTag -> aTag.attr("href")).forEach(linkPool::add);
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
