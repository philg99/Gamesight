package org.gamesight.dao;

import java.util.List;
import java.util.Optional;

import org.gamesight.dto.ProfileDto;
import org.gamesight.dto.UserDto;
import org.gamesight.exception.ResourceAlreadyExistsException;
import org.gamesight.exception.ResourceNotFoundException;
import org.gamesight.model.Profile;
import org.gamesight.model.User;
import org.gamesight.repository.ProfileRepository;
import org.gamesight.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// Declare class as provider of Spring beans.
@Configuration
public class UserDao {
	/*
	The UserDao class provides all access to the underlying persistence repository.
	It is used by the domain business DTO objects for persistence CRUD and query
	operations.
	 */
	/*
	Persistence strategy: the interfaces of the Spring JpaRepository will be used.
	(See https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.entity-persistence).
	(See also: https://www.baeldung.com/hibernate-save-persist-update-merge-saveorupdate).
	A null identifier property will be used to mark a new entity, and the save() method
	will be used to both create a new entity or update an existing one. Even though Spring
	will also use the identifier property to determine whether an update or create operation
	is needed, this DAO will also provide explicit 'create' or 'update' methods to
	force the calling application to know whether an entity does/should already exist or not?
	This will reduce accidental creation of new entities coming from detached states.
	 */
	/*
	Patterns used in this DAO:
		1. All references to a JpaRepository are limited to the DAO classes. No other
		application classes should directly use a JpaRepository interface. This
		encapsulation will make it easier to swap out JPA impls, if needed?
		2. All references to the application model classes (User.java, Profile.java, etc.)
		are limited to the DAO classes. No public DAO methods accept or return these
		classes. All public interfaces only use the DTO classes.
	 */

	private final UserRepository userRepository;
	private final ProfileRepository profileRepository;
	private final ProfileDao profileDao;
	private final ModelMapper mapper = new ModelMapper();


	UserDao(UserRepository userRepository, ProfileRepository profileRepository,
			ProfileDao profileDao) {

		this.userRepository = userRepository;
		this.profileRepository = profileRepository;
		this.profileDao = profileDao;

		TypeMap<Profile, ProfileDto> profileToProfileDtoMapping =
				mapper.createTypeMap(Profile.class, ProfileDto.class);
		profileToProfileDtoMapping.addMapping(Profile::getId, ProfileDto::setId);

		TypeMap<UserDto, User> userDtoToUserMapping =
				mapper.createTypeMap(UserDto.class, User.class);
		userDtoToUserMapping.addMapping(UserDto::getEmailAddress, User::setEmailAddress);
		userDtoToUserMapping.addMapping(UserDto::getId, User::setId);

		TypeMap<UserDto, Profile> userDtoToProfileMapping =
				mapper.createTypeMap(UserDto.class, Profile.class);

		TypeMap<User, UserDto> userToUserDtoMapping =
				mapper.createTypeMap(User.class, UserDto.class);

		TypeMap<Profile, UserDto> profileToUserDtoMapping =
				mapper.createTypeMap(Profile.class, UserDto.class);
		profileToUserDtoMapping.addMapping(Profile::getId, UserDto::setProfileId);

	}

	public boolean isUniqueEmailAddress(String emailAddress) {
		List<User> userList = userRepository.findByEmailAddress(emailAddress);
		return ((userList == null) || (userList.size() == 0));
	}

	public UserDto createUser(UserDto userDto) throws ResourceAlreadyExistsException {
		// User entity requires an associated 1:1 Profile to exist.
		userDto = profileDao.createUserAndProfile(userDto);
		return userDto;
	}

	public UserDto updateUser(UserDto userDto) throws ResourceNotFoundException {
		// Update an already existing User (with Profile) entity.
		if (userDto.getId() == null) {
			throw new ResourceNotFoundException("User updateUser() request with null id");
		}
		User user = mapFromUserDto(userDto);
		// Note: must save the Profile before saving the User (with embedded Profile).
		// Saving a User with a modified, embedded Profile simply reloads the profile
		// without saving it first therefore losing any modifications.
		profileRepository.save(user.getProfile());
		user = userRepository.save(user);
		UserDto newUserDto = mapToUserDto(user);

		return newUserDto;

	}

	/*
	Note that this method is private so the User model persistence class
	is not exposed outside the DAO class.
	 */
	private User mapFromUserDto(UserDto userDto) {
		// Transform a data transfer object into an entity object used for
		// persistence store interactions.

		User newUser = new User();
		if (userDto.getId() != null) {
			// Retrieve existing User first for this update operation.
			newUser = userRepository.findById(userDto.getId()).get();
			// Overwrite any updated attributes from the DTO.
			// Note: fetching the User will also retrieve the embedded Profile
			mapper.map(userDto, newUser);
			// Note: Updating the embedded Profile object, and then simply saving the
			// parent User object, does NOT automatically propagate a save onto the Profile.
			mapper.map(userDto, newUser.getProfile());
		}
		return newUser;
	}

	public Page<User> findAll(Pageable pageable) {
		Page<User> userPage =  userRepository.findAll(pageable);
		return userPage;
	}

	public List<User> findAll() {
		List<User> userList =  userRepository.findAll();
		return userList;
	}


	public Optional<UserDto> findById(Long id) {
		if ( id == null) {
			throw new ResourceNotFoundException("User updateUser() request with null id");
		}

		Optional<User> optUser =  userRepository.findById(id);
		if (!optUser.isPresent())  {
			return null;
		} else {
			User user = optUser.get();
			UserDto userDto = new UserDto();
			UserDto newUserDto = mapToUserDto(user);
			return Optional.of(newUserDto);
		}
	}

	private UserDto mapToUserDto(User user)  {
		// Note: Profile->UserDto mapping is overwriting userDto.userId erroneously.
		// Must do User->UserDto afterwards to reset it.
		UserDto userDto = new UserDto();
		mapper.map(user.getProfile(), userDto);
		mapper.map(user, userDto);
		return userDto;
	}

	public void deleteById(Long id) {
		if ( id== null) {
			throw new ResourceNotFoundException("User deleteById() request with null id");
		}
		userRepository.deleteById(id);
	}
}
