package org.gamesight.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gamesight.dao.ProfileDao;
import org.gamesight.dto.ProfileDto;
import org.gamesight.exception.PatchFieldUnsupportedException;
import org.gamesight.exception.ResourceAlreadyExistsException;
import org.gamesight.exception.ResourceNotFoundException;
import org.gamesight.model.Profile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;


@RestController
public class ProfileController {


	/*
	The ProfileController provides the REST CRUD services for the Profile entity.
	 */
	private final ProfileDao profileDao;

	private static final Logger logger = LogManager.getLogger(ProfileController.class);

	ProfileController(ProfileDao profileDao) {
		this.profileDao = profileDao;
	}
	/*
	Get all existing Profile records.
	 */

	// TODO - Consider rewriting page-based processing with hypermedia support:
	// https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#core.web

	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successful operation")
	})
	@GetMapping("/api/v1/mgmt/profile")
	FindAllWrapper findAllProfiles(@RequestParam(required = false, name = "pageNum") Integer pageNum,
			@RequestParam(required = false, name = "pageSize", defaultValue = "5") Integer pageSize,
			@RequestParam(required = false, name = "sortBy", defaultValue = "dob") String sortBy,
			@RequestParam(required = false, name = "sortDir", defaultValue = "ASC") String sortDir) {

		/*
		Since we can only map a single GetMapping impl against our /profile operations,
		but we want to alternatively return List<Profile or Page<Profile>, use a wrapper
		class. The existence (or not) of the paging params will determine whether this is
		a paged request.
		 */
		if (pageNum == null) {
			//  Non-paged request:
			List<Profile> profileList = Optional.of(profileDao.findAll()).
					orElseThrow(() -> new ResourceNotFoundException("finaAllProfiles failed"));
			FindAllWrapper findAllWrapper = new FindAllWrapper();
			findAllWrapper.setProfileList(profileList);
			return findAllWrapper;
		}
		else {
			// Paged request:
			Sort sort = (sortDir == null || sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())) ?
					Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
			Pageable pageable = PageRequest.of(pageNum, pageSize, sort);

			Page<Profile> profilePage = Optional.of(profileDao.findAll(pageable)).
					orElseThrow(() -> new ResourceNotFoundException("findAllProfiles paged failed"));

			return new FindAllWrapper(pageable, profilePage.getContent());
		}
	}

	// TODO: Add @Valid support for Profile request param
		/*
		Save a new Profile.
		 */
	@PostMapping(value = "/api/v1/mgmt/profile")
	//return 201 instead of 200 to signify resource was created
	@ResponseStatus(HttpStatus.CREATED)
	ProfileDto createProfile(@RequestBody ProfileDto profileDto) {

		ProfileDto returnVal = null;
		try {
			logger.info("Create Profile request ProfileDto: : {} ", () -> profileDto);
			returnVal = Optional.of(profileDao.createProfile(profileDto))
					.orElseThrow(() -> new ResourceNotFoundException("createProfile failed"));
		} catch (ResourceAlreadyExistsException raee) {
			logger.warn("Create Profile from profileDto failed: {} ", () -> profileDto);
			throw new ResourceNotFoundException("createProfile failed - resource already exists");
		}
		logger.info("Create Profile response ProfileDto: : " +  returnVal);
		return returnVal;
	}

	/*
	Get Profile by id.
	 */
	@GetMapping("/api/v1/mgmt/profile/{id}")
	Optional<ProfileDto> getProfile(@PathVariable Long id) {
		return Optional.ofNullable(profileDao.findById(id)    // Return found object else throw exception
				.orElseThrow(() -> new ResourceNotFoundException(id)));
	}

	/*
	 Update the entire Profile via a PUT request
	 */
	@PutMapping("/api/v1/mgmt/profile/{id}")
	Optional<ProfileDto> updateProfile(@RequestBody ProfileDto profileDto, @PathVariable Long id) {

		logger.info("Update Profile request id {} ProfileDto {} ", id, profileDto);



		return Optional.of(profileDao.findById(id)
				.map(dto -> {
					dto.setStreet(profileDto.getStreet());
					dto.setCity(profileDto.getCity());
					dto.setState(profileDto.getState());
					dto.setZip(profileDto.getZip());
					dto.setDob(profileDto.getDob());
					dto.setId(id);

					logger.info("Update Profile findById rsp: ProfileDto {} ", dto);
					ProfileDto response = profileDao.updateProfile(dto);
					logger.info("Update Profile updateProfile rsp: ProfileDto {} ", response);
					return response;

				})
				.orElseThrow(() -> new ResourceNotFoundException(id)));


	}

	/*
	 Patch a given field in the Profile
	 */
	@PatchMapping("/api/v1/mgmt/profile/{id}")
	Optional<ProfileDto> patchProfile(@RequestBody Map<String, String> patch, @PathVariable Long id) {

		return Optional.of(profileDao.findById(id)
				.map(dto -> {

					// TODO: iterate on map keySet and Switch on requested fields
					String state = patch.get("state");
					if (!(state == null)) {
						// Update the requested field(s) on the current Profile and persist/patch
						// those fields.
						dto.setState(state);
						return profileDao.updateProfile(dto);
					}
					else {
						throw new PatchFieldUnsupportedException(patch.keySet());
					}
				})
				.orElseThrow(() -> new ResourceNotFoundException(id)));
	}

	@DeleteMapping("/api/v1/mgmt/profile/{id}")
	void deleteProfile(@PathVariable Long id) {

		profileDao.deleteById(id);
	}

}
