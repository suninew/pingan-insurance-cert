package com.pingan.pinganproject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * App
 *
 */
public class App {
	public static WebDriver driver;
	public static String newTab, userNameString, userPasswordString, browserAddress;
	public static String cookieSting = "";
	public static String polNo = "";
	public static ArrayList<String> problemIdList = new ArrayList<>();
	public static HashMap<String, String> customerRecordMap = new HashMap<>();
	public static int sleepTime = 2;

	public static void main(String[] args) throws Exception {
		String webUrlString = "https://yl.pingan.com/#/qylogin";
		// Initial config File
		initial_config_file();

		System.setProperty("webdriver.chrome.driver", "file/chromedriver.exe");
		ChromeOptions options = new ChromeOptions();

		options.setBinary(browserAddress);

		try {
			driver = new ChromeDriver(options);
			driver.get(webUrlString);
			Thread.sleep(2000);
			WebElement iframElement = driver
					.findElement(By.xpath("/html/body/div[1]/section/main/div/div/div/div[2]/iframe"));
			driver.switchTo().frame(iframElement);
			driver.findElement(By.id("userName")).sendKeys(userNameString);
			driver.findElement(By.id("passwordInput")).sendKeys(userPasswordString);
			Thread.sleep(8000);
			driver.findElement(By.id("submitButton")).click();

			Thread.sleep(3000);
			// Switch to new Window
			switch_to_new_window();
			Thread.sleep(1000);
			get_cookies();
			// Clicked Cancel button
			try {
				driver.findElement(By.xpath("//div[@id='authTips']/div/input[4]")).click();
			} catch (Exception e) {
				// todo
			}
			negative_to_check();

			for (String key : customerRecordMap.keySet()) {
				String idNo = customerRecordMap.get(key);

				try {
					WebElement custIdElement = driver.findElement(By.xpath("//*[@id=\"queryValue\"]"));
					custIdElement.clear();
					custIdElement.sendKeys(idNo);
					// click confirm to continue
					driver.findElement(By.xpath("//*[@id=\"c\"]/form/div[3]/a")).click();
					Thread.sleep(sleepTime * 1000);

					// *[@id="items"]
					String certNo = "";
					String insureName = "";
					ArrayList<Integer> matchItemsList = new ArrayList<>();
					try {
						List<WebElement> certTrElementList = driver
								.findElements(By.xpath("//form/table[3]/tbody[@id='items']/tr"));
						String insureUrlPdf = "";
						if (certTrElementList.size() > 2) {

							for (int i = 1; i < certTrElementList.size(); i++) {
								if (driver.findElement(By.xpath("//form/table[3]/tbody[1]/tr[" + i + "]/td[7]/span"))
										.getText().equalsIgnoreCase("缴费有效"))
									matchItemsList.add(i);
							}
							for (Integer i : matchItemsList) {
								certNo = driver.findElement(By.xpath("//*[@id='tdCertNo" + i + "']")).getText();
								// insuredName : get the Chines name and convert to GBK
								insureName = toGbkString(
										driver.findElement(By.xpath("//*[@id='tdClientName" + i + "']")).getText());

								insureUrlPdf = "https://pa-ssl.pingan.com/cspi-internet/org/pension/insuranceCertificateExportPdf.do?polNo="
										+ polNo + "&insuredName=" + insureName + "&IdNo=" + idNo + "&certNo=" + certNo;

								if (matchItemsList.size() > 1) {
									download_pdf(insureUrlPdf, key + "_" + i);
									System.out
											.println("Downloaded file for " + idNo + " to " + key + "_" + i + ".pdf!");
								} else {
									download_pdf(insureUrlPdf, key);
									System.out.println("Downloaded file for " + idNo + " to " + key + ".pdf!");
								}
							}

//							String certNo1Status = driver
//									.findElement(By.xpath("//form/table[3]/tbody[1]/tr[1]/td[7]/span")).getText();
//							String certNo2Status = driver
//									.findElement(By.xpath("//form/table[3]/tbody[1]/tr[2]/td[7]/span")).getText();
//							if (certNo1Status.equalsIgnoreCase("缴费有效")) {
//								certNo = driver.findElement(By.xpath("//*[@id=\"tdCertNo1\"]")).getText();
//								// insuredName : get the Chines name and convert to GBK
//								insureName = toGbkString(
//										driver.findElement(By.xpath("//*[@id=\"tdClientName1\"]")).getText());
//
//							} else if (certNo2Status.equalsIgnoreCase("缴费有效")) {
//								certNo = driver.findElement(By.xpath("//*[@id=\"tdCertNo2\"]")).getText();
//								// insuredName : get the Chines name and convert to GBK
//								insureName = toGbkString(
//										driver.findElement(By.xpath("//*[@id=\"tdClientName2\"]")).getText());
//							}
//							;
						} else {
							certNo = driver.findElement(By.xpath("//*[@id=\"tdCertNo1\"]")).getText();
							// insuredName : get the Chines name and convert to GBK
							insureName = toGbkString(
									driver.findElement(By.xpath("//*[@id=\"tdClientName1\"]")).getText());

							insureUrlPdf = "https://pa-ssl.pingan.com/cspi-internet/org/pension/insuranceCertificateExportPdf.do?polNo="
									+ polNo + "&insuredName=" + insureName + "&IdNo=" + idNo + "&certNo=" + certNo;

							download_pdf(insureUrlPdf, key);
							System.out.println("Downloaded file for " + idNo + " to " + key + ".pdf!");

						}
					} catch (Exception e) {

					}
				} catch (NoSuchElementException e) {
					System.out.println("Fail to query customer: " + key + "," + idNo + "!");
					problemIdList.add(customerRecordMap.get(key));
					negative_to_check();
				}
			}

			// Start to try to download the pdf
			System.out.println("Finished Download Job !");
			Thread.sleep(2000);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			driver.close();
			driver.quit();
		}
	}

	private static void initial_config_file() throws Exception, FileNotFoundException {
		ArrayList<String> arrayList = new ArrayList<>();
//Read configure from file/config.txt
		File configFile = new File("file/config.txt");
		InputStreamReader inputReader = new InputStreamReader(new FileInputStream(configFile), "UTF-8");
		BufferedReader bf = new BufferedReader(inputReader);
		String str;

		while ((str = bf.readLine()) != null) {
			if (!str.equalsIgnoreCase("")) {
				arrayList.add(str);
			}
		}
		bf.close();
		inputReader.close();
		int i = 0;
		for (String record : arrayList) {
			record_format_check(record);
			if (i == 0) {
				userNameString = record.split(",")[0];
				userPasswordString = record.split(",")[1];
				polNo = record.split(",")[2];
				browserAddress = record.split(",")[3];
				sleepTime = Integer.valueOf(record.split(",")[4]);
				i++;
			} else
				customerRecordMap.put(record.split(",")[0], record.split(",")[1]);
		}
	}

	private static void record_format_check(String record) {
		if ((record.split(",").length != 2) && (record.split(",").length != 5)) {
			System.out.println("Please make sure correct records have been passed from line " + record + "!!");
			System.exit(0);
		}
	}

	private static void download_pdf(String insureUrlPdf, String customerId) throws IOException {
		URL url = new URL(insureUrlPdf);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36 QIHU 360SE/13.1.1456.0");
		conn.setRequestProperty("Cookie", cookieSting);
		conn.connect();
		InputStream inputStream = conn.getInputStream();

		byte[] buffer = new byte[1024];
		int len = 0;
		DataInputStream bis = new DataInputStream(inputStream);
		File saveDir = new File("file/download/");
		if (!saveDir.exists())
			saveDir.mkdir();

		File file = new File(saveDir + File.separator + customerId + ".pdf");
		FileOutputStream fileOut = new FileOutputStream(file);
		DataOutputStream bos = new DataOutputStream(fileOut);
		while ((len = inputStream.read(buffer, 0, 1024)) != -1) {
			bos.write(buffer, 0, len);
		}
		bos.close();
		bis.close();
		conn.disconnect();
	}

	private static void get_cookies() {
		Set<Cookie> cookies = driver.manage().getCookies();
		for (Cookie cookie : cookies) {
			cookieSting = cookieSting + cookie.getName() + "=" + cookie.getValue() + ";";
		}
	}

	private static void negative_to_check() throws InterruptedException {
		Thread.sleep(1000);
		driver.switchTo().defaultContent();
		driver.switchTo().frame(driver.findElement(By.xpath("//*[@id=\"EM_PAGE\"]")));

		// Click 保险凭证
		driver.findElement(By.xpath(
				"//*[@id=\"class1content\"]/div/div/div[2]/ul/li/a[@href=\"/cspi-internet/org/pension/InsuranceCertificateDownLoad.do\"]"))
				.click();

		boolean acceptFlag = true;
		try {
			driver.switchTo().alert().accept();
		} catch (NoAlertPresentException e) {
			acceptFlag = false;
		} finally {
			// 再次点击保险凭证下载
			if (acceptFlag)
				driver.findElement(By.xpath(
						"//*[@id=\"class1content\"]/div/div/div[2]/ul/li/a[@href=\"/cspi-internet/org/pension/InsuranceCertificateDownLoad.do\"]"))
						.click();
		}
		Thread.sleep(2000);
		// Switched to iFrames
		driver.switchTo().frame(driver.findElement(By.xpath("//*[@id=\"content\"]")));
		Thread.sleep(1000);
		// 选择保单
		// Check all to see if could locate the 保单
		WebElement polNoTablElement;
		try {
			polNoTablElement = driver.findElement(By.xpath("//*[@id=\"c\"]/form/table[1]"));
		} catch (NoSuchElementException e) {
			System.out.println("Could not see any valid PolNo!!");
			System.exit(0);
		}
		driver.findElement(By.xpath("//*[@id=\"c\"]/form/table[1]/tbody/tr[2]/td[1]/input")).click();
		// 选择身份证按钮
		driver.findElement(By.xpath("//*[@id=\"c\"]/form/table[2]/tbody/tr[1]/td[3]/input")).click();
	}

	private static void switch_to_new_window() {
		Set<String> handleSet = driver.getWindowHandles();
		String currentString = driver.getWindowHandle();
		handleSet.remove(currentString);
		newTab = handleSet.iterator().next();
		driver.switchTo().window(newTab);
	}

	public static String toGbkString(String s) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= 0 && c <= 255) {
				sb.append(c);
			} else {
				byte[] b;
				try {
					b = String.valueOf(c).getBytes("gbk");
				} catch (Exception ex) {
					System.out.println(ex);
					b = new byte[0];
				}
				for (int j = 0; j < b.length; j++) {
					int k = b[j];
					if (k < 0)
						k += 256;
					sb.append("%" + Integer.toHexString(k).toUpperCase());
				}
			}
		}
		return sb.toString();
	}

	public static byte[] readInputStream(InputStream inputStream) throws IOException {

		byte[] buffer = new byte[1024];
		int len = 0;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		while ((len = inputStream.read(buffer)) != -1) {
			bos.write(buffer, 0, len);
		}
		bos.close();
		return bos.toByteArray();
	}

}
