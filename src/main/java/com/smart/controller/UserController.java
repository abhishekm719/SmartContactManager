package com.smart.controller;

import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entity.Contact;
import com.smart.entity.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository; 
	
	@Autowired
	private ContactRepository contactRepository; 
	
	
	//
	@ModelAttribute
	public void adCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME "+userName);
		
		User user = userRepository.getUserByUserName(userName);
		
		System.out.println("USER "+user);
		
		model.addAttribute("user",user);
	}
	
	
	//dashboard home
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		model.addAttribute("title", "User Dashboard");

		return "normal/user_dashboard";
	}
	//open add form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());

		return "normal/add_contact_form";
	}
	
	//processing add contact form
	@PostMapping("/process-contact")
	public String processContact(
			@ModelAttribute Contact contact,
			@RequestParam("profileImage")MultipartFile file, 
			Principal principal, HttpSession session) {
		
		try {
		String name = principal.getName();
		User user = userRepository.getUserByUserName(name);
		//processing and uploading file..
		if(file.isEmpty()) {
			System.out.println("File is Empty");
			contact.setImage("contact.png");
			
		}else {
			contact.setImage(file.getOriginalFilename());
			
			File saveFile= new ClassPathResource("static/img").getFile();
			Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
			Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
			System.out.println("Image uploaded");
		}
		
		user.getContacts().add(contact);
		contact.setUser(user);
		
		this.userRepository.save(user);
		
		System.out.println("DATA" + contact);
		System.out.println("Added to database");
		session.setAttribute("message", new Message("Your contact is added !!","success"));
		
		}catch(Exception e) {
			System.out.println("ERROR "+e.getMessage());
			e.printStackTrace();
			session.setAttribute("message", new Message("Something went wrong !! try again","danger"));

		}
		
		return "normal/add_contact_form";
	}
	
	
	//show contact handler
	@GetMapping("/show-contacts/{page}")

	public String showContacts(@PathVariable("page")Integer page,Model m,Principal principal) {
		m.addAttribute("title", "Show User Contacts");

		String userName = principal.getName();
		User user = userRepository.getUserByUserName(userName);

		Pageable pageable=PageRequest.of(page, 2);
		Page<Contact> contacts=this.contactRepository.findContactByUser(user.getId(),pageable);
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());
		return "normal/show_contacts";
	}
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model,Principal principal) {
		System.out.println("CID "+ cId);
		
		Optional<Contact> contactOptional=this.contactRepository.findById(cId);
		Contact contact=contactOptional.get();
		
		String userName= principal.getName();
		User user=this.userRepository.getUserByUserName(userName);
		
		if(user.getId()==contact.getUser().getId()) {
		model.addAttribute("contact",contact);
		model.addAttribute("title",contact.getName());

		}
		return "normal/contact_detail";
	}
	
	//delete contact handler
	
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId,Model model,Principal principal, HttpSession session) {
		Optional<Contact> contactOptional=this.contactRepository.findById(cId);
		
		Contact contact=contactOptional.get();
		
		String userName= principal.getName();
		User user=this.userRepository.getUserByUserName(userName);
		user.getContacts().remove(contact);
		this.userRepository.save(user);
		session.setAttribute("message", new Message ("Contact deleted successfully...", "success"));
		return "redirect:/user/show-contacts/0";
		
	}
	
	//open update form handler
	
	@PostMapping("/update-contact/{cId}")

	public String updateForm(Model m,@PathVariable("cId") Integer cid) {
		
		
		m.addAttribute("title","Update Contact");
		
		Contact contact= this.contactRepository.findById(cid).get();
		
		m.addAttribute("contact",contact);
		return "normal/update_form";
	}
	
	
	//update contact handler
	@PostMapping("/process-update")

	public String updateHandler(@ModelAttribute Contact contact,
			
		@RequestParam("profileImage")MultipartFile file, 
		Model m, HttpSession session, Principal principal) {
		
		try {
			
			//old contact details
			Contact oldcontactDetail= this.contactRepository.findById(contact.getcId()).get();
			
			//image....
			if(!file.isEmpty()) {
				
				// delete old photo
				File deleteFile= new ClassPathResource("static/img").getFile();
				File file1= new File(deleteFile, oldcontactDetail.getImage());
				file1.delete();
				
				// update old photo
				File saveFile= new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
				Files.copy(file.getInputStream(),path,StandardCopyOption.REPLACE_EXISTING);
				contact.setImage(file.getOriginalFilename());
				
				
			}else {
				contact.setImage(oldcontactDetail.getImage());
				
			}
			User user = this.userRepository.getUserByUserName(principal.getName());
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your contact id updated...","success"));
		}
		catch (Exception e) {e.printStackTrace();}
		
		System.out.println("CONTACT NAME "+contact.getName());
		System.out.println("CONTACT ID "+contact.getcId());

		return "redirect:/user/"+contact.getcId()+"/contact";
		
		
	}
	
}
