package telran.courses.service;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import telran.courses.dto.Course;
import telran.courses.exceptions.ResourceNotFoundException;
import static telran.courses.api.ApiConstants.*;
@Service
public class CoursesServiceImpl implements CoursesService, Serializable{
/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
private static final int MILLIS_IN_MINUTE = 60000;
@Value("${app.interval.minutes: 1}")
	int interval;
   boolean flUpdate = false;
	static Logger LOG = LoggerFactory.getLogger(CoursesService.class);
	
	@Value("${app.file.name: courses.data}")
	String fileName;	
		
		
	private Map<Integer, Course> courses = new HashMap<>();
	@Override
	public  Course addCourse(Course course) {
	    course.id = generateId();
	    Course res = add(course);
	    flUpdate = true;
	    return res;
	}

	private Course add(Course course) {
		courses.put(course.id, course);
		return course;
	}

	

	

	@Override
	public List<Course> getAllCourses() {
	    
	    return new ArrayList<>(courses.values());
	}

	
	private Integer generateId() {
	    ThreadLocalRandom random = ThreadLocalRandom.current();
	    int randomId;

	    do {
	        randomId = random.nextInt(MIN_ID, MAX_ID);
	    } while (exists(randomId));
	    return randomId;
	}

	private boolean exists(int id) {
		return courses.containsKey(id);
	}

	@Override
	public Course getCourse(int id) {
		Course course = courses.get(id);
		if (course == null) {
			throw new ResourceNotFoundException(String.format("course with id %d not found", id));
		}
		return course;
	}

	@Override
	public Course removeCourse(int id) {
		Course course = courses.remove(id);
		if (course == null){
			throw new ResourceNotFoundException(String.format("course with id %d not found", id));
		}
		flUpdate = true;
		return course;
	}

	@Override
	public Course updateCourse(int id, Course course) {
		
		Course courseUpdated = courses.replace(id, course);
		if (courseUpdated == null){
			throw new ResourceNotFoundException(String.format("course with id %d not found", id));
		}
		flUpdate = true;
		return courseUpdated;
	}

	
	private void restore() {
		try(ObjectInputStream input = new ObjectInputStream(new FileInputStream(fileName))){
			Map<Integer, Course> coursesRestored = (Map<Integer, Course>) input.readObject();
			courses = coursesRestored;
			LOG.debug("service has been restored from file {}", fileName);
			
		} catch(FileNotFoundException e) {
			LOG.warn("service has not been restored - no file {} found", fileName);
			
		} 
		catch (Exception e) {
			LOG.warn("service has not been restored by nested exception {} ", e.toString());
		}
		
	}

	
	private boolean save() {
		
		if (flUpdate) {
			try(ObjectOutputStream output =
					new ObjectOutputStream(new FileOutputStream(fileName))) {
					output.writeObject(courses);
					flUpdate = false;
					return true;
				
			} catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			}
		}
		return false; 
		
		
	}
	@PostConstruct
	void restoreInvocation() {
		restore();
		Thread thread = new Thread(() -> {
			LOG.debug("Thread {} created ", Thread.currentThread().getName());
			boolean isSaved = false;
			while(true) {
				try {
					Thread.sleep(interval * MILLIS_IN_MINUTE);
					isSaved = save();
				} catch (InterruptedException e) {
					
				}
				if (isSaved) {
					LOG.debug("courses data saved into file {}", fileName);
				} else {
					LOG.debug("courses have not been updated, no saved");
				}
				
			}
			
		});
		thread.setDaemon(true);
		thread.start();
	}
	@PreDestroy
	void persistCoursesData() {
		boolean isSaved = save();
		
		if (isSaved) {
			LOG.debug("courses data saved into file {}", fileName);
		} else {
			LOG.debug("courses have not been updated, no saved");
		}
		
	}
	}


