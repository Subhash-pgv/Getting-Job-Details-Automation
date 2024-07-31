package WebScrapers;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.interactions.Actions;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JobScrapping2 {
    public static void main(String[] args) throws IOException, InterruptedException, SQLException, ClassNotFoundException {

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--window-size=1920x1080");
        options.addArguments("--disable-gpu");
        WebDriver driver = new ChromeDriver(options);
        Actions actions = new Actions(driver);

        driver.get("https://weworkremotely.com/remote-jobs/search?search_uuid=&term=&sort=any_time&categories%5B%5D=2&categories%5B%5D=17&categories%5B%5D=18");
        driver.manage().window().maximize();
        sleepRandom();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

      //sql connection set up
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

		String connectionURL = "jdbc:sqlserver://10.0.2.34:1433;Database=Automation;User=mailscan;Password=MailScan@343260;encrypt=true;trustServerCertificate=true";

		Connection connection = DriverManager.getConnection(connectionURL);
		
	   String insertSQL = "INSERT INTO JobListings (jobTitle, jobLocation, jobUrl, companyName) VALUES (?, ?, ?, ?)";
       String checkSQL = "SELECT COUNT(*) FROM jobListings WHERE jobUrl = ?";
   
       
        wait.until(ExpectedConditions
                .presenceOfElementLocated(By.xpath("//div//ul//li/a//span[@class='title']")));

       List<WebElement> totalJobs = driver.findElements(By.xpath("//div//ul//li/a//span[@class='title']"));
       int totalJobCount = totalJobs.size();

        int[] sections = {2, 17, 18};
        int totalJobsAppended = 0;
        int totalJobFinds =0;
        
        for (int sectionId : sections) {
        	 System.out.println("Adding JObs to DB please wait untill it shows completed.....");
        	List<WebElement> resultCountElement = driver.findElements(By.xpath("//section[@id='category-" + sectionId + "']//li/a//span[@class='title']"));
   
        	for (int i = 1; i <= resultCountElement.size(); i++) {
        
                String companyName = "";
                String jobTitle = "";
                String jobLocation = "";
                String jobURL = "";

                // Handle each element and check if it exists
                WebElement companyNames = getElementIfExists(driver, "(//section[@id='category-" + sectionId + "']//li[" + i + "]/a//span[@class='company'][1])");
                if (companyNames != null) {
                    companyName = companyNames.getText();
                }

                WebElement jobTitles = getElementIfExists(driver, "(//section[@id='category-" + sectionId + "']//li[" + i + "]/a//span[@class='title'])");
                if (jobTitles != null) {
                    jobTitle = jobTitles.getText();
                }



                WebElement jobLocations = getElementIfExists(driver, "(//section[@id='category-" + sectionId + "']//li[" + i + "]/a//span[@class='region company'])");
                if (jobLocations != null) {
                    jobLocation = jobLocations.getText();
                }

                WebElement jobURLs = getElementIfExists(driver, "(//section[@id='category-" + sectionId + "']//li[" + i + "]/a//span[@class='region company'])/parent::a");
                if (jobURLs != null) {
                    jobURL = jobURLs.getAttribute("href");
                }
                
                
                
             // Check if job URL already exists
                PreparedStatement checkStatement = connection.prepareStatement(checkSQL);
                checkStatement.setString(1, jobURL);
                ResultSet resultSet = checkStatement.executeQuery();
                if (resultSet.next() && resultSet.getInt(1) == 0) {
                    // Insert new job listing
                    PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                    insertStatement.setString(1, jobTitle);
                    insertStatement.setString(2, jobLocation);
                    insertStatement.setString(3, jobURL);
                    insertStatement.setString(4, companyName);
                    insertStatement.executeUpdate();
                    insertStatement.close();
                    
                    totalJobsAppended++;
                }
                
                totalJobFinds++;
                resultSet.close();
                checkStatement.close();
    
            }
        }
        
        if (totalJobCount == totalJobFinds) {
            System.out.println("Searched all companies for new jobs");

            if (totalJobCount == totalJobsAppended) {
                System.out.println("All (" + totalJobCount + ") companies' jobs added to DB successfully.");
            } else if (totalJobsAppended > 0) {
                System.out.println(totalJobsAppended + " jobs added to DB successfully.");
            } else {
                System.out.println("No new jobs found");
               
            }
        }
        
        driver.quit(); // Make sure to quit the WebDriver
        connection.close();
    }

    private static WebElement getElementIfExists(WebDriver driver, String xpath) {
        try {
            List<WebElement> elements = driver.findElements(By.xpath(xpath));
            if (elements.size() > 0) {
                return elements.get(0);
            }
        } catch (NoSuchElementException e) {
            // Element is not found, return null
        }
        return null;
    }

    private static void sleepRandom() {
        try {
            int delay = new Random().nextInt(2000) + 1000; // Delay between 1 and 2 seconds
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}