package org.gamesight.test.standalone;

import java.time.LocalDate;
import java.time.Month;

import org.gamesight.dao.ProfileDao;
import org.gamesight.dao.UserDao;
import org.gamesight.dto.ProfileDto;
import org.gamesight.exception.ResourceAlreadyExistsException;
import org.gamesight.model.Profile;
import org.gamesight.repository.ProfileRepository;
import org.gamesight.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class UserAndProfileMockTests {


	// User Profile data:
	String street = "100 Maple Drive";

	String city = "Springfield";

	String state = "Missouri";

	String stateUpdate = "Pennsylvania";

	int zip = 10020;

	String emailAddress = "John.Doe@gmail.com";

	LocalDate dob = LocalDate.of(2000, Month.NOVEMBER, 29);

	// User test data:
	String userName = "Test User Name";


	@Mock
	private UserRepository userRepository;
	@Mock
	private ProfileRepository profileRepository;

	@InjectMocks
	private ProfileDao profileDaoService;

	@InjectMocks
	private UserDao userDaoService;

	@Test
	void testCreateProfile() {

		ProfileDto profileDto = new ProfileDto(null, city, street, state, zip, dob);

		Profile profile = new Profile();
		profile.setCity(city);

		when(profileRepository.save(any())).thenReturn(profile);

		try {
			ProfileDto profileDtoResult = profileDaoService.createProfile(profileDto);
			assertEquals(profile.getCity(), profileDtoResult.getCity());
		} catch (ResourceAlreadyExistsException resourceAlreadyExistsException) {
			// else fail
		}
	}
}
