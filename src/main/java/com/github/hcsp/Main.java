package com.github.hcsp;


import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

import java.util.stream.Collectors;


public class Main {

    @SuppressFBWarnings("DMI_CONSTANT_DB_PASSWORD")
    public static void main(String[] args) throws IOException, SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:file:C:/Users/25224/Desktop/xiedaimala-crawler/target/xiedaimala-crawler", "root", "root");
        String link;
        while ((link = loadUrlsFromDataBaseAndUpdateDataBase(connection)) != null) {
            if (isProcessedLink(connection, link)) {
                continue;
            }
            if (isWannaLink(link)) {
                Document doc = parseHtmlAndgetDocument(link);
                getPotentialLinkAndSaveToWhatWillBeDealtDatebase(doc, connection);
                saveToNewsDatabase(link, connection, doc);
                InsertIntoProcessedDatabase(connection, link, "INSERT INTO LINKED_ALREADY_PROCESSED (LINK) values ( ? )");
            }
        }
    }

    private static void saveToNewsDatabase(String link, Connection connection, Document document) throws SQLException {
        ArrayList<Element> articles = document.select("article");
        if (!articles.isEmpty()) {
            for (Element article : articles) {
                System.out.println(articles.get(0).child(0).text());
                try (PreparedStatement preparedStatement = connection.prepareStatement("insert into NEWS(TITLE, CONTENT, URL, CREATED_AT, MODIFIED_AT) values (?, ?, ?, now(), now())")) {
                    preparedStatement.setString(1, articles.get(0).child(0).text());
                    ArrayList<Element> paragraphs = article.select("p");
                    String content = article.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                    preparedStatement.setString(2, content);
                    preparedStatement.setString(3, link);
                    preparedStatement.executeUpdate();

                }

            }
        }
    }

    private static Document parseHtmlAndgetDocument(String link) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        System.out.println(link);
        HttpGet httpGet = new HttpGet(link);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36");
        CloseableHttpResponse response1 = httpclient.execute(httpGet);
        return getAndParseHtml(response1);
    }

    private static String loadUrlsFromDataBaseAndUpdateDataBase(Connection connection) throws SQLException {
        String link = loadUrlsFromDatabase(connection, "select LINK from LINKS_TO_BE_PROCESSED limit 1");
        if (link == null) {
            return null;
        }
        updateDatabase(connection, link, "DELETE  FROM LINKS_TO_BE_PROCESSED where LINK = ?");
        return link;
    }

    private static boolean isProcessedLink(Connection connection, String link) throws SQLException {
        boolean flag = false;
        ResultSet resultSet = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT  LINK FROM LINKED_ALREADY_PROCESSED where link = ?")) {
            preparedStatement.setString(1, link);
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                flag = true;
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return flag;
    }


    private static void getPotentialLinkAndSaveToWhatWillBeDealtDatebase(Document doc, Connection connection) throws SQLException {
        ArrayList<Element> links = doc.select("a");
        for (Element aTag : links) {
            String href = aTag.attr("href");
            InsertIntoDatabaseWhatWillBeDealtWith(connection, href, "INSERT INTO LINKS_TO_BE_PROCESSED (LINK) values ( ? )");
        }
    }

    private static void InsertIntoProcessedDatabase(Connection connection, String href, String s) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(s)) {
            preparedStatement.setString(1, href);
            preparedStatement.executeUpdate();
        }
    }

    private static void InsertIntoDatabaseWhatWillBeDealtWith(Connection connection, String href, String s) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(s)) {
            preparedStatement.setString(1, href);
            preparedStatement.executeUpdate();
        }
    }

    private static void updateDatabase(Connection connection, String href, String s) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(s)) {
            preparedStatement.setString(1, href);
            preparedStatement.executeUpdate();
        }
    }

    private static Document getAndParseHtml(CloseableHttpResponse response1) throws IOException {
        HttpEntity entity1 = response1.getEntity();
        String html = EntityUtils.toString(entity1);
        return Jsoup.parse(html);
    }

    private static boolean isWannaLink(String link) {
        return ((link.contains("news.sina.cn")) || (link.equals("https://sina.cn"))) && ((!link.startsWith("https:\\/\\/")) && (!link.startsWith("https://passport")));

    }

    private static String loadUrlsFromDatabase(Connection connection, String sql) throws SQLException {
        ResultSet resultSet = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return null;
    }
}

