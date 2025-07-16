package com.example.aialibaba.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Service
public class InformationService {

    @Autowired
    @Qualifier("openAiChatClient")
    private ChatClient chatClient;

    @Autowired
    private DeepSeekChatModel deepSeekChatModel;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private OpenAiImageModel openAiImageModel;

    public Matcher getMatcher(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        return matcher;
    }

    public Map<String, String> getMsg(String text) {
        String phoneRegex = "(1[3-9]\\d{9})" +                     // 手机号
                "|([a-zA-Z0-9_-]+@[a-zA-Z0-9]+(\\.[a-zA-Z]+){1,2})" +  // 邮箱
                "|(0\\d{2,3}-?[1-9]\\d{6,7})" +          // 座机
                "|(400-?618-?\\d{4})";                  // 400热线
        Matcher matcher = getMatcher(text, phoneRegex);
        String phone = null;
        String email = null;
        while (matcher.find()) {
            String group = matcher.group();
            if (group.contains("@")) {
                email = group;
            }else{
                phone = group;
            }
        }
        Map<String, String> result = new HashMap<>();
        result.put("phone", phone == null ? "未识别到手机号" : phone.toString());
        result.put("email", email == null ? "未识别到邮箱" : email.toString());
        return result;
    }

    public Map<String, String> getSex(String text) {
        String sexRegex = "(?<![\\u4e00-\\u9fa5])[男女](?![\\u4e00-\\u9fa5])";                  // 400热线
        Matcher matcher = getMatcher(text, sexRegex);
        String sex = null;
        while (matcher.find()) {
            String group = matcher.group();
            sex = group;
        }
        Map<String, String> result = new HashMap<>();
        result.put("sex", sex == null ? "未识别到性别" : sex.toString());
        return result;
    }

    public Map<String, List<String>> getEdu(String text) {
        String eduRegex = "(博士|硕士|本科|专科|大专|研究生|高中|职高|中专|技校|初中|小学)|([\\u4e00-\\u9fa5]+(大学|学院))";
        Matcher matcher = getMatcher(text, eduRegex);
        List<String>list = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group();
            list.add(group);
        }
        Map<String, List<String>> result = new HashMap<>();
        result.put("edu", list.size() == 0 ? Arrays.asList("未识别到学历") : list);
        return result;
    }
    public Map<String, String> getFullName(String text) {
        // 提取前两行
        Map<String, String> map = new HashMap<>();
        String[] lines = text.split("\\n", 3);
        String firstLine = lines.length > 0 ? lines[0].trim() : "";
        String secondLine = lines.length > 1 ? lines[1].trim() : "";
        String[] template = {"的", "个人简历", "简历"};
        String fullName = null;
        for(String tmp : template){
            int idx = firstLine.indexOf(tmp);
            if(idx == -1) continue;
            fullName = firstLine.substring(0, idx);
            break;
        }
        if(fullName != null){
            map.put("fullName", fullName);
            return map;
        }

        return null;
    }

    public String getText1(String path) throws IOException {
        PDDocument document = Loader.loadPDF(new File(path));
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        return text;
    }
    public String getText2(String path) throws IOException {
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(path))) {
            StringBuilder extractedText = new StringBuilder();
            for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
                extractedText.append(PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i))).append("\n");
            }
            String text = new String(extractedText.toString());

            return text;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public String getJson(String text) {
        StringBuilder jsonBuilder = new StringBuilder();
        int cnt = 0;
        boolean flag = false;
        for(int i = 0; i < text.length(); i++){
            char c = text.charAt(i);
            if(c == '{' && !flag){
                flag = true;
            }
            if(flag){
                if(c == '{') cnt ++ ;
                else if(c == '}') cnt -- ;
                jsonBuilder.append(c);
                if(cnt == 0) break;
            }
        }
        String json = jsonBuilder.toString();
        return json;
    }
    public String getInformation() throws IOException {
        String path = "D:\\IdeaProject\\ai-alibaba\\src\\main\\resources\\xzc.pdf";
        String text = getText2(path);
        text = text.replaceFirst("^.*?\\n", "");
        String query = "输出上述简历文件的姓名，电话，邮箱，工作年限，最高学历，就读学校，项目经历，项目经历包括名称，时间和内容，内容做个简单介绍即可，格式为json，比如{\"name\":\"小帅\"}，不要输出任何多余内容，只返回json即可，深度思考的部分也去掉";
//        String answer = chatClient.prompt(text + query)
//                .call()
//                .content();
//        String response = deepSeekChatModel.call(text + query);
        String response = chatClient.prompt()
                .user(text + query)
                .call()
                .content();
//        return response;
        String json = getJson(response);
        return json;
    }
}
