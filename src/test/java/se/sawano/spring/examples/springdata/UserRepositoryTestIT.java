/*
 * Copyright  2012 Daniel Sawano
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package se.sawano.spring.examples.springdata;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Integration tests for the {@link UserRepository}. Run in your IDE or with Maven: mvn clean verify
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-config.xml")
public class UserRepositoryTestIT {

    private final int NO_OF_USERS = 50;

    @Autowired
    private UserRepository userRepository;
    private ArrayList<User> testUsers;

    @Before
    public void setUp() throws Exception {
        testUsers = createTestUsers();
        saveTestUsersToRepository();
    }

    private ArrayList<User> createTestUsers() {
        ArrayList<User> users = new ArrayList<User>();
        for (int i = 0; i < NO_OF_USERS; ++i) {
            users.add(new User("John" + i, "Doe" + i));
        }
        return users;
    }

    private void saveTestUsersToRepository() {
        userRepository.save(testUsers);
        for (User user : testUsers) {
            assertNotNull(user.getId());
        }
    }

    @After
    public void tearDown() throws Exception {
        userRepository.deleteAll();
        assertEquals(0, userRepository.count());
    }

    @Test
    public void findAllShouldReturnAllUsers() throws Exception {
        List<User> users = userRepository.findAll();

        assertEquals("Wrong number of users found", NO_OF_USERS, users.size());
        assertEquals("Wrong number of users found", testUsers.size(), users.size());
    }

    @Test
    public void deleteByIdShouldRemoveAUserFromTheRepository() throws Exception {
        final Long id = testUsers.get(0).getId();

        userRepository.delete(id);

        List<User> users = userRepository.findAll();

        User template = new User();
        template.setId(id);
        assertFalse("User should be deleted", users.contains(template));
        assertEquals("Wrong number of users found", NO_OF_USERS - 1, users.size());
        assertFalse("User should be deleted", userRepository.exists(id));
        assertNull("User should be deleted", userRepository.findOne(id));
    }

    @Test
    public void deleteManyShouldDeleteTheSpecifiedUsers() throws Exception {
        userRepository.delete(testUsers);

        assertEquals(0, userRepository.count());
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void repositoryShouldCheckNullConstraintForFirstName() throws Exception {
        User user = new User(null, "LastName");
        try {
            userRepository.save(user);
            fail();
        }
        catch (DataIntegrityViolationException e) {
            assertTrue("Could not find column name in error message", e.getMessage().contains("USER column: FIRSTNAME"));
            throw e;
        }
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void repositoryShouldCheckNullConstraintForLastName() throws Exception {
        User user = new User("FirstName", null);
        try {
            userRepository.save(user);
            fail();
        }
        catch (DataIntegrityViolationException e) {
            assertTrue("Could not find column name in error message: ", e.getMessage().contains("USER column: LASTNAME"));
            throw e;
        }
    }

    @Test
    public void findByNameShouldReturnMatchingUsers() throws Exception {
        final String name = "John1";

        final List<User> matches = userRepository.findByFirstNameOrderByLastNameAsc(name);

        assertNotNull(matches);
        assertEquals(1, matches.size());
        assertEquals(name, matches.get(0).getFirstName());

        userRepository.save(new User(name, "DoeB"));
        final List<User> matches2 = userRepository.findByFirstNameOrderByLastNameAsc(name);

        assertNotNull(matches2);
        assertEquals(2, matches2.size());
        assertEquals("Doe1", matches2.get(0).getLastName());
        assertEquals("DoeB", matches2.get(1).getLastName());
    }

    @Test
    public void getByFirstNameLikeShouldReturnMatchingUsers() throws Exception {
        List<User> matches = userRepository.getByFirstNameLike("%ohn%"); // Note the SQL symbol '%'

        assertEquals(NO_OF_USERS, matches.size());
    }
}
