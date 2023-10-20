package com.inn.cafe.serviceImpl;

import com.inn.cafe.JWT.CustomerUserDetailsService;
import com.inn.cafe.JWT.JwtFilter;
import com.inn.cafe.JWT.JwtUtill;
import com.inn.cafe.POJO.User;
import com.inn.cafe.constants.CafeConstants;
import com.inn.cafe.dao.UserDao;
import com.inn.cafe.service.UserService;
import com.inn.cafe.utils.CafeUtils;
import com.inn.cafe.utils.EmailUtils;
import com.inn.cafe.wrapper.UserWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserDao userDao;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    CustomerUserDetailsService customerUserDetailsService;
    @Autowired
    JwtUtill jwtUtill;
    @Autowired
    JwtFilter jwtFilter;
    @Autowired
    EmailUtils emailUtils;
    @Override
   public ResponseEntity<String> signUp(Map<String,String> requestMap){
     log.info("inside the signUp {}",requestMap);
     try {
         if (validateRequestMap(requestMap)) {
             User user = userDao.findByEmailID(requestMap.get("email"));
             if (Objects.isNull(user)) {
                 userDao.save(getUserfromMap(requestMap));
                 CafeUtils.getResponseEntity("Successfully registered", HttpStatus.OK);

             } else {
                 CafeUtils.getResponseEntity("Email already exists.", HttpStatus.BAD_REQUEST);
             }


         } else {
             return CafeUtils.getResponseEntity(CafeConstants.INVALID_DATA, HttpStatus.BAD_REQUEST);
         }
     }catch (Exception ex){
         ex.printStackTrace();

     }
        return CafeUtils.getResponseEntity(CafeConstants.SOMETHING_WENT_WRONG,HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private boolean validateRequestMap(Map<String ,String> requestMap){
        if(requestMap.containsKey("name") && requestMap.containsKey("contactNumber")
                && requestMap.containsKey("email") && requestMap.containsKey("password")){
            return  true;
        }
        return false;
    }
    private User getUserfromMap(Map<String,String> requestMap){
        User user = new User();
        user.setName(requestMap.get("name"));
        user.setContactNumber(requestMap.get("contactNumber"));
        user.setEmail(requestMap.get("email"));
        user.setPassword(requestMap.get("password"));
        user.setStatus("false");
        user.setRole("user");
        return user;
    }
    @Override
    public ResponseEntity<String> Login(Map<String, String> requestMap) {
        log.info("inside Login");
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(requestMap.get("email"), requestMap.get("password"))
            );
            if (auth.isAuthenticated()) {
                if (customerUserDetailsService.getUserDetail().getStatus().equals("true")) {
                    return new ResponseEntity<String>("{\"token\":\"" +
                            jwtUtill.generateToken(customerUserDetailsService.getUserDetail().getEmail(),
                                    customerUserDetailsService.getUserDetail().getRole()) + "\"}", HttpStatus.OK);
                } else
                {
                    return new ResponseEntity<String>("{\"message\":\"wait for admin approval.\"}", HttpStatus.BAD_REQUEST);
                }
            }

        } catch (Exception ex) {
            log.error("{}", ex);
        }
        return new ResponseEntity<String>("{\"message\":\"Bad credential.\"}", HttpStatus.BAD_REQUEST);
    }
    @Override
    public ResponseEntity<List<UserWrapper>> getAllUser() {
        try{
            if (jwtFilter.isAdmin()) {
                return new ResponseEntity<>(userDao.getAllUser(),HttpStatus.OK);
            }
            return  new ResponseEntity<>(new ArrayList<>(),HttpStatus.UNAUTHORIZED);

        }catch (Exception ex){
            ex.printStackTrace();
        }
        return new ResponseEntity<List<UserWrapper>>(new ArrayList<>(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @Override
    public ResponseEntity<String> update(Map<String, String> requestMap) {
      try {
          if (jwtFilter.isAdmin()) {
              Optional<User> optionalUser = userDao.findById(Integer.parseInt(requestMap.get("id")));
              if (!optionalUser.isEmpty()){
                  userDao.updateStatus(requestMap.get("status"),Integer.parseInt(requestMap.get("id")));
                  sendMailtoAllAdmin(requestMap.get("status"),optionalUser.get().getEmail(),userDao.getAllAdmin());
                  return CafeUtils.getResponseEntity("User status updated successfully",HttpStatus.OK);

          }else {
                  return CafeUtils.getResponseEntity("user Id does not exist", HttpStatus.OK);
              }
          }
          else {
              return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS,HttpStatus.INTERNAL_SERVER_ERROR);
          }
      }catch (Exception e) {
        e.printStackTrace();
      }
        return CafeUtils.getResponseEntity(CafeConstants.UNAUTHORIZED_ACCESS,HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void sendMailtoAllAdmin(String status, String user, List<String> allAdmin) {
        allAdmin.remove(jwtFilter.getCurrentUser());
        if (status != null && status.equalsIgnoreCase("true")){
            emailUtils.sendSimpleMessage(jwtFilter.getCurrentUser(),"Account Approved","User:- " + user + " \n is approved by \nADMIN:-" + jwtFilter.getCurrentUser(),allAdmin);
        }
    }
}
