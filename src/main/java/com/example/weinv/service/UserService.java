package com.example.weinv.service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.weinv.entity.Otp;
import com.example.weinv.entity.User;
import com.example.weinv.persistence.OtpRepo;
import com.example.weinv.persistence.UserRepo;

import jakarta.transaction.Transactional;

@Service
public class UserService {
	
	@Autowired
	private UserRepo userRepo;
	
	@Autowired
	private OtpRepo otpRepo;
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private PasswordEncoder passEnc;

	@Transactional
	public void initiate_registration(String email) {
		LocalDateTime init = LocalDateTime.now();
		LocalDateTime exp =  init.plusMinutes(10);
		
		//OTP ENTRY
		Otp new_otp = new Otp();
		new_otp.setEmail(email);
		new_otp.setOtp(randomNumber());
		new_otp.setInit_time(Timestamp.valueOf(init));
		new_otp.setExp_time(Timestamp.valueOf(exp));
		otpRepo.save(new_otp);
		
		//USER ENTRY
		User user = new User();
		user.setEmail(email);
		userRepo.save(user);
		
		//MAIL NOTIFICATION
		emailService.sendEmail("weinv@help.com", new_otp.getEmail(), "Verify Yourself", "Your OTP is READY", "<p>Your OTP is "+ Integer.toString(new_otp.getOtp())+"</p>");
	}
	
	@Transactional
	public boolean verify_registration(int otp){
		List<Object[]> res = otpRepo.verify_reg(otp);
		Otp otpobj = new Otp();

		for(Object[] row : res) {
			otpobj.setEmail((String) row[1]);
			otpobj.setOtp((int) row[2]);
			otpobj.setInit_time((Timestamp) row[3]) ;
			otpobj.setExp_time((Timestamp) row[4]);
		}
		if(isOtpValid(otpobj.getInit_time().toLocalDateTime(),otpobj.getExp_time().toLocalDateTime())) {
			userRepo.verifyUser(1, otpobj.getEmail());
			return true;
		}
		return false;
	}
	
	@Transactional
	public boolean setUpPassword(String email,String password) {
		Optional<User> userdet =  userRepo.findByEmail(email);
		if(userdet.isPresent()) {
			User user = userdet.get();
			int is_verified = user.getVerified();
			
			if(is_verified != 0) {
				userRepo.updatePassword( email, passEnc.encode(password));
				return true;
			}
		}else {
			System.out.println("User is not verfied!");
		}
		return false;
		
	}

	public boolean isOtpValid(LocalDateTime init , LocalDateTime exp) {
		Duration dif = Duration.between(init, exp);
		if(dif.toMinutes() <= 0) {
			return false;
		}
		return true;
	}
	
	public int randomNumber() {
		int min_value = 100_000;
		int max_value = 999_999;
		
		Random random = new Random();
		return random.nextInt(max_value - min_value + 1) + min_value;
	}
	
	public User getUserByid(int id) {
		return userRepo.findById(id).get();
	}
	
}
