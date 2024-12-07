package org.gamesight.controller;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gamesight.dao.UserDao;
import org.gamesight.dto.UserDto;
import org.gamesight.exception.DuplicateResourceException;
import org.gamesight.exception.ResourceAlreadyExistsException;
import org.gamesight.exception.ResourceNotFoundException;
import org.gamesight.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

	/*
	The UserController provides the REST CRUD services for the User entity.
	 */
	private final UserDao userDao;

	private static final Logger logger = LogManager.getLogger(UserController.class);
	private static final int DEF_PAGE_SIZE = 5;

	UserController(UserDao userDao) {

		this.userDao = userDao;
	}

	/*
	Get all existing User records.
	 */
	@GetMapping("/api/v1/mgmt/user")
	// TODO: pageable object contains an invalid Sort string. Swagger issue?
	Page<User> findAll(@RequestParam(required = false, name = "pageNum", defaultValue = "0") Integer pageNum,
			@RequestParam(required = false, name = "pageSize", defaultValue = "5") Integer pageSize,
			@RequestParam(required = false, name = "sortBy",  defaultValue = "emailAddress" ) String sortBy,
			@RequestParam(required = false, name = "sortDir", defaultValue = "ASC") String sortDir)  {

		// Paged request :
		Sort sort = (sortDir == null || sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())) ?
				Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

		Pageable pageable = PageRequest.of(pageNum, pageSize, sort);
		return userDao.findAll(pageable);
	}

	// TODO: Add @Valid support for user request param

	/*
	Save a new User.
	 */
	@PostMapping(value = "/api/v1/mgmt/user")
	//return 201 instead of 200
	@ResponseStatus(HttpStatus.CREATED)
	UserDto newUser(@RequestBody UserDto userDto) {

		// Validate the requested User email address and then create a new User.
		try {
			if ((userDto.getEmailAddress().length() != 0) && userDao.isUniqueEmailAddress(userDto.getEmailAddress())) {
				return userDao.createUser(userDto);
			} else {
				throw new DuplicateResourceException(userDto.getEmailAddress());
			}

		} catch (ResourceAlreadyExistsException raee) {
			logger.warn("Create User from userDto failed: {} ", () -> userDto);
		}
		return null;
	}

	/*
	Find User by Id.
	 */
	@GetMapping("/api/v1/mgmt/user/{id}")
	Optional<UserDto> getUser(@PathVariable Long id) {
		// Return found object else throw exception
		Optional<UserDto> userDto = Optional.ofNullable(userDao.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException(id)));
		return userDto;
	}


	@DeleteMapping("/api/v1/mgmt/user/{id}")
	void deleteUser(@PathVariable Long id) {
		userDao.deleteById(id);
	}

	// TODO: add PUT api

}
