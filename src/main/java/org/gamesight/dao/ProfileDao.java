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
public class ProfileDao {

	/*
	The ProfileDao class provides all access to the underlying persistence repository.
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

	private final ProfileRepository profileRepository;
	private final UserRepository userRepository;
	private final ModelMapper mapper = new ModelMapper();


	ProfileDao(ProfileRepository profileRepository, UserRepository userRepository) {
		this.profileRepository = profileRepository;
		this.userRepository = userRepository;

		TypeMap<Profile, ProfileDto> profileToProfileDtoMapping =
				mapper.createTypeMap(Profile.class, ProfileDto.class);
		profileToProfileDtoMapping.addMapping(Profile::getId, ProfileDto::setId);

		TypeMap<ProfileDto,Profile > profileDtoToProfileMapping =
				mapper.createTypeMap(ProfileDto.class, Profile.class);
		profileDtoToProfileMapping.addMapping(ProfileDto::getId, Profile::setId);

		TypeMap<Profile, UserDto> profileToUserDtoMapping =
				mapper.createTypeMap(Profile.class, UserDto.class);
		profileToUserDtoMapping.addMapping(Profile::getId, UserDto::setProfileId);
	}

	public UserDto createUserAndProfile(UserDto userDto) throws ResourceAlreadyExistsException {
		// Create method should not find existing entity id's.
		if ((userDto.getId() != 0) && (userDto.getId() != null)) {
			throw new ResourceAlreadyExistsException("User entity", userDto.getId());
		} else {
			if ((userDto.getProfileId()!= 0) && (userDto.getProfileId()!= null)) {
				throw new ResourceAlreadyExistsException("Profile entity", userDto.getProfileId());
			}
		}
		// User entity requires an associated 1:1 Profile
		Profile profile = profileRepository.save(mapFromUserDto(userDto));
		User user = new User();
		user.setEmailAddress(userDto.getEmailAddress());
		user.setProfile(profile);
		userRepository.save(user);
		mapper.map(user, userDto);
//		mapper.map(profile,userDto);	// Not needed? Apparently, deep mapping already occurred to set profileId
		return userDto;

	}

	public ProfileDto createProfile(ProfileDto profileDto) throws ResourceAlreadyExistsException {
		// Create method should not find existing entity id's.
		if (profileDto.getId()!= 0) {
			throw new ResourceAlreadyExistsException("Profile entity", profileDto.getId());
		}
		Profile newProfile = profileRepository.save(mapFromProfileDto(profileDto));
		return mapFromProfile(newProfile);

	}

	public ProfileDto updateProfile(ProfileDto profileDto) {
		if (profileDto.getId() == null) {
			throw new ResourceNotFoundException("Profile updateProfile() request with null id");
		}
		// Update an already existing Profile entity.
		Profile profile = profileRepository.save(mapFromProfileDto(profileDto));
		ProfileDto newProfileDto= new ProfileDto();
		mapper.map(profile, newProfileDto);
		return newProfileDto;
	}

	private Profile mapFromUserDto(UserDto userDto) {
		// Transform a data transfer object into an entity object used for
		// persistence store interactions for an UPDATE action.
		Profile profile = new Profile();
		mapper.map(userDto, profile);
		return profile;
	}

	/*
	Note that this method is private so the Profile model persistence class
	is not exposed outside the DAO class.
	 */
	private Profile mapFromProfileDto(ProfileDto profileDto) {
		// Transform a data transfer object into an entity object used for
		// persistence store interactions.

		Profile profile = new Profile();
		mapper.map(profileDto, profile);
		return profile;

	}

	/*
	Note that this method is private so the Profile model persistence class
	is not exposed outside the DAO class.
 	*/
	private ProfileDto mapFromProfile(Profile profile) {
		ProfileDto profileDto = new ProfileDto();
		mapper.map(profile, profileDto);
		return profileDto;
	}

	public void deleteById(Long id) {
		profileRepository.deleteById(id);
	}

	// Do not expose the *save* method since it handles both create and update
	// operations, and we need to proxy those requests with integrity checks.
//	public Profile save(Profile profile) {
//		return profileRepository.save(profile);
//	}

	public Optional<ProfileDto> findById(Long id) {
		if (id == null) {
			throw new ResourceNotFoundException("Profile findById() request with null id");
		}
		Optional<Profile> optProfile =  profileRepository.findById(id);
		if (!optProfile.isPresent()) {
			return Optional.ofNullable(null);
		} else {
			Profile profile = optProfile.get();
			ProfileDto profileDto = new ProfileDto();
			mapper.map(profile, profileDto);
			return Optional.of(profileDto);
		}
	}

	public List<Profile> findAll() {
		return profileRepository.findAll();
	}

	public Page<Profile> findAll(Pageable pageable) {
		return profileRepository.findAll(pageable);
	}
}
