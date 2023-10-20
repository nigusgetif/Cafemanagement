package com.inn.cafe.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailUtils {
    @Autowired
    private JavaMailSender javaMailSender;

    public void sendSimpleMessage(String to, String subj, String txt, List<String> list){
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom("kn@gmail.com");
        simpleMailMessage.setSubject(subj);
        simpleMailMessage.setTo(to);
        simpleMailMessage.setText(txt);
        if (list != null && list.size() > 0)
           simpleMailMessage.setCc(getCcArray(list));
        javaMailSender.send(simpleMailMessage);
    }

    public String[] getCcArray(List<String> lst){
        String[] cc = new String[lst.size()];
        for (int i = 0;i<lst.size();i++){
            cc[i]= lst.get(i);
        }
        return cc;
        }


}
