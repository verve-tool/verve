package verve;
/*
 * Created by Ibrahim Althomali on 12/15/2017
 * 
 ********************************************/


import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.assertthat.selenium_shutterbug.core.Shutterbug;
import com.assertthat.selenium_shutterbug.utils.web.ScrollStrategy;

import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.screentaker.ViewportPastingStrategy;


public class DriverManager
{
	Dimension maxDim;
	Dimension minDim;
	Dimension maxJSDim;
	Dimension minJSDim;
	WebDriver driver; 
	BufferedImage screenshot;
	String local = "file:" + File.separator + File.separator + File.separator;
	File currentDir = null;
	JavascriptExecutor jsExc;
	int scrollX = 0;
	int scrollY = 0;
	int cantReachX = 0;
	int cantReachY = 0;
	String returnType;
public DriverManager(String browser)
{
	screenshot = null;

	String ext = ""; 
	try {
		currentDir = new File(".").getCanonicalFile();
	} catch (IOException e) {
		e.printStackTrace();
	}
	System.out.println(currentDir);
	if(File.separator.equals("\\")) //Windows Path
	{
		ext = ".exe";
	}
	if(browser.toLowerCase().equals("edge"))
	{
	    System.setProperty("webdriver.edge.driver", currentDir + File.separator +"resources"+ File.separator +"MicrosoftWebDriver" + ext);
	}
	else if(browser.toLowerCase().equals("phantomjs"))
	{
	    System.setProperty("phantomjs.binary.path", currentDir + File.separator +"resources"+ File.separator +"phantomjs" + ext);
	}
	else if(browser.toLowerCase().equals("chrome"))
	{
	    System.setProperty("webdriver.chrome.driver", currentDir + File.separator +"resources"+ File.separator +"chromedriver" + ext);
	    startChrome();
	}
	else if(browser.toLowerCase().equals("firefox"))
	{
	    System.setProperty("webdriver.gecko.driver", currentDir + File.separator +"resources"+ File.separator +"geckodriver" + ext);
	    //startFirefox();
	    startFirefoxRedecheckStyle();
	}
	else
	{
		System.out.println("The browser choice ("+browser+") is not recognized. Exiting...");
		System.exit(0);
	}
}
public void startFirefox() {
	driver = new FirefoxDriver();
	setWait(8);
	driver.get(local + currentDir + File.separator +"resources"+ File.separator +"ibrahim.html");
	jsExc = (JavascriptExecutor)driver;	
	measureScreen();
}
public void startFirefoxRedecheckStyle()
{
	String fullURL = local + currentDir + File.separator +"resources"+ File.separator +"ibrahim.html";
	driver = new FirefoxDriver();
	setWait(8);
	jsExc = (JavascriptExecutor)driver;	
    // Load up the webpage in the browser, using a pop-up to make sure we can resize down to 320 pixels wide
    String winHandleBefore = driver.getWindowHandle();
    driver.get(fullURL);
    ((JavascriptExecutor) driver).executeScript("var newwindow=window.open(\"" + fullURL + "\",'test','width=320,height=1024,top=50,left=50', scrollbars='no', menubar='no', resizable='no', toolbar='no', top='+top+', left='+left+', 'false');\n" +
            "newwindow.focus();");
    for(String winHandle : driver.getWindowHandles()){
    	driver.switchTo().window(winHandle);
        if (winHandle.equals(winHandleBefore)) {
        	driver.close();
        }
    }
    driver.manage().window().setPosition(new Point(0,0));
    measureScreen();
    //Assist.pause("popup should be open");
}
public void startChrome() {
	driver = new ChromeDriver();
	setWait(8);
	driver.get(local + currentDir + File.separator +"resources"+ File.separator +"ibrahim.html");
	jsExc = (JavascriptExecutor)driver;	
	measureScreen();
}
public String getURL()
{
	return driver.getCurrentUrl();
}
public void measureScreen() {
	driver.manage().window().setPosition(new Point(0,0));
	driver.manage().window().setSize(new Dimension(0,0));
	minDim = driver.manage().window().getSize();
	minJSDim = new Dimension(Integer.parseInt(jsExc.executeScript("return window.innerWidth;").toString()), Integer.parseInt(jsExc.executeScript("return window.innerHeight;").toString()));
	System.out.println("Min Height: " + minDim.getHeight() + "-"+ minJSDim.getHeight() + " ... Min Width: " + minDim.getWidth() +"-"+ minJSDim.getWidth());
	driver.manage().window().maximize();
	maxDim = driver.manage().window().getSize();
	maxJSDim = new Dimension(Integer.parseInt(jsExc.executeScript("return window.innerWidth;").toString()), Integer.parseInt(jsExc.executeScript("return window.innerHeight;").toString()));
	System.out.println("Max Height: " + maxDim.getHeight() + "-"+ maxJSDim.getHeight() +" ... Max Width: " + maxDim.getWidth() +"-"+ maxJSDim.getWidth());
	driver.manage().window().setSize(new Dimension(minDim.getWidth(),maxDim.getHeight()));
	Object obj = jsExc.executeScript("return window.pageXOffset;");
	if(obj instanceof Long)
	{
		returnType = "long";
	}
	else if(obj instanceof Double)
	{
		returnType = "double";
	}
}
public void maximize()
{
	driver.manage().window().maximize();
}
private void setWait(int maxWaitSeconds)
{
	driver.manage().timeouts().implicitlyWait(maxWaitSeconds,TimeUnit.SECONDS);
}
public void navigate(String url)
{
	driver.get(url);
}
public void setViewport(int viewport) {
	//int height = driver.manage().window().getSize().getHeight();
	int height = 1000; //To match tom experiment;
	driver.manage().window().setSize(new Dimension(viewport, height));
	Assist.waitToLoad();

}
public BufferedImage saveFullScreenshot(String fileName)
{
	BufferedImage screenshot = new AShot().shootingStrategy(new ViewportPastingStrategy(Assist.viewportPatingWait)).takeScreenshot(driver).getImage();
	//BufferedImage screenshot = Shutterbug.shootPage(driver, ScrollStrategy.WHOLE_PAGE).getImage();
	try {
		ImageIO.write(screenshot, "png", new File(Assist.outDirectory + File.separator + fileName));
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return screenshot;
}
public void saveViewportScreenshot(String fileName)
{
	try {
		ImageIO.write(ImageIO.read(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)), "png", new File(Assist.outDirectory + File.separator + fileName));
	} catch (IOException e) {
		e.printStackTrace();
	}
}
public BufferedImage screenshot()
{
//    WebDriver augmentedDriver = new Augmenter().augment(driver);
//	TakesScreenshot screenshotTaker = (TakesScreenshot)augmentedDriver;
//	try {
//		screenshot = ImageIO.read(screenshotTaker.getScreenshotAs(OutputType.FILE));
//	
//	} catch (WebDriverException e) {
//		e.printStackTrace();
//	} catch (IOException e) {
//		e.printStackTrace();
//	}
//	return screenshot;
	
	File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
	BufferedImage img = null;
	try 
	{
		img = ImageIO.read(screenshotFile);
	} catch (IOException e) {
		e.printStackTrace();
	}
	return img;
	
}
public String getOpacity(WebElement element)
{
	String script = "return window.getComputedStyle(arguments[0]).getPropertyValue(\"opacity\");";
	String opacity = (String) jsExc.executeScript(script, element);
	return opacity;
}
public void setOpacity(WebElement element, String opacity)
{
	String script = "arguments[0].style.opacity = \""+ opacity +"\";";
	jsExc.executeScript(script, element); 
	Assist.waitToSettle();
}
public String getPosition(WebElement element)
{
	String script = "return window.getComputedStyle(arguments[0]).getPropertyValue(\"position\");";
	String position = (String) jsExc.executeScript(script, element);
	return position;
}
public void setPosition(WebElement element, String position)
{
	String script = "arguments[0].style.position = \""+ position +"\";";
	jsExc.executeScript(script, element); 
}
public String getTop(WebElement element)
{
	String script = "return window.getComputedStyle(arguments[0]).getPropertyValue(\"top\");";
	String top = (String) jsExc.executeScript(script, element);
	return top;
}
public void setTop(WebElement element, String top)
{
	String script = "arguments[0].style.top = \""+ top +"\";";
	jsExc.executeScript(script, element); 
}
public String getLeft(WebElement element)
{
	String script = "return window.getComputedStyle(arguments[0]).getPropertyValue(\"left\");";
	String left = (String) jsExc.executeScript(script, element);
	return left;
}
public void setLeft(WebElement element, String left)
{
	String script = "arguments[0].style.left = \""+ left +"\";";
	jsExc.executeScript(script, element); 
}
public String getMarginLeft(WebElement element)
{
	String script = "return window.getComputedStyle(arguments[0]).getPropertyValue(\"margin-left\");";
	String marginLeft = (String) jsExc.executeScript(script, element);
	return marginLeft;
}
public void setMarginLeft(WebElement element, String marginLeft)
{
	String script = "arguments[0].style.marginLeft = \""+ marginLeft +"\";";
	jsExc.executeScript(script, element); 
}
public String getMarginRight(WebElement element)
{
	String script = "return window.getComputedStyle(arguments[0]).getPropertyValue(\"margin-right\");";
	String marginRight = (String) jsExc.executeScript(script, element);
	return marginRight;
}
public void setMarginRight(WebElement element, String marginRight)
{
	String script = "arguments[0].style.marginRight = \""+ marginRight +"\";";
	jsExc.executeScript(script, element); 
}
public String getMarginTop(WebElement element)
{
	String script = "return window.getComputedStyle(arguments[0]).getPropertyValue(\"margin-top\");";
	String marginTop = (String) jsExc.executeScript(script, element);
	return marginTop;
}
public void setMarginTop(WebElement element, String marginTop)
{
	String script = "arguments[0].style.marginTop = \""+ marginTop +"\";";
	jsExc.executeScript(script, element); 
}
public String getMarginBottom(WebElement element)
{
	String script = "return window.getComputedStyle(arguments[0]).getPropertyValue(\"margin-bottom\");";
	String marginBottom = (String) jsExc.executeScript(script, element);
	return marginBottom;
}
public void setMarginBottom(WebElement element, String marginBottom)
{
	String script = "arguments[0].style.marginTop = \""+ marginBottom +"\";";
	jsExc.executeScript(script, element); 
}
public void setStyle(WebElement element, String style)
{
	String script = "arguments[0].removeAttribute(\"style\");";
	String script2 = "arguments[0].setAttribute(\"style\", \""+style+"\");";
	jsExc.executeScript(script, element); 
	jsExc.executeScript(script2, element); 
}
public String getStyle(WebElement element)
{
	String script = "return arguments[0].getAttribute(\"style\");";
	String style = (String) jsExc.executeScript(script, element);
	return style;
}
public String getWidth(WebElement element)
{
	String script = "return window.getComputedStyle(arguments[0]).getPropertyValue(\"width\");";
	String marginTop = (String) jsExc.executeScript(script, element);
	return marginTop;
}
public void setWidth(WebElement element, String width)
{
	String script = "arguments[0].style.width = \""+ width +"\";";
	jsExc.executeScript(script, element); 
}
public String getHeight(WebElement element)
{
	String script = "return window.getComputedStyle(arguments[0]).getPropertyValue(\"height\");";
	String marginTop = (String) jsExc.executeScript(script, element);
	return marginTop;
}
public void setHeight(WebElement element, String height)
{
	String script = "arguments[0].style.height = \""+ height +"\";";
	jsExc.executeScript(script, element); 
}
public String getPaddingLeft(WebElement element)
{
	String script = "return window.getComputedStyle(arguments[0]).getPropertyValue(\"padding-left\");";
	String paddingLeft = (String) jsExc.executeScript(script, element);
	return paddingLeft;
}
public void setPaddingLeft(WebElement element, String paddingLeft)
{
	String script = "arguments[0].style.paddingLeft = \""+ paddingLeft +"\";";
	jsExc.executeScript(script, element); 
}
public String getPaddingTop(WebElement element)
{
	String script = "return window.getComputedStyle(arguments[0]).getPropertyValue(\"padding-top\");";
	String paddingTop = (String) jsExc.executeScript(script, element);
	return paddingTop;
}
public void setPaddingTop(WebElement element, String paddingTop)
{
	String script = "arguments[0].style.paddingTop = \""+ paddingTop +"\";";
	jsExc.executeScript(script, element); 
}
public String getBackgroundColor(WebElement element)
{
	String script = "return window.getComputedStyle(arguments[0]).getPropertyValue(\"background-color\");";
	String backgroundColor = (String) jsExc.executeScript(script, element);
	return backgroundColor;
}
public void setBackgroundColor(WebElement element, String backgroundColor)
{
	String script = "arguments[0].style.backgroundColor = \""+ backgroundColor +"\";";
	jsExc.executeScript(script, element); 
}

public void scrollZero()
{

	scrollX = 0;
	scrollY = 0;
	
	int offsetX = 0;
	int offsetY = 0;
	jsExc.executeScript("scroll("+ scrollX +","+ scrollY +");"); //scroll to bottom right point of rectangle into view
	//System.out.println("Scrolled to: " + scrollX +","+ scrollY);
	Object obj = jsExc.executeScript("return window.pageXOffset;");
	if(obj instanceof Long)
	{
		offsetX = Math.toIntExact((Long) obj);
	}
	else if(obj instanceof Double)
	{
		offsetX = ((Double) obj).intValue();
	}
	obj = jsExc.executeScript("return window.pageYOffset;");
	if(obj instanceof Long)
	{
		offsetY = Math.toIntExact((Long) obj);
	}
	else if(obj instanceof Double)
	{
		offsetY = ((Double) obj).intValue();
	}
	//System.out.println("Offset   to: " + offsetX +","+ offsetY);
	
	if((scrollX - offsetX) > 0)
	{
		cantReachX = scrollX - offsetX;
	}
	else
	{
		cantReachX = 0;
	}
	if((scrollY - offsetY) > 0)
	{
		cantReachY = scrollY - offsetY;
	}
	else
	{
		cantReachY = 0;
	}
	scrollX = offsetX;
	scrollY = offsetY;
	Assist.waitToSettle();
}









public WebElement getWebElem(String xpath)
{
	WebElement e = null;
	try
	{
		e = driver.findElement(By.xpath(xpath));
	}catch(Exception NoSuchElementException)
	{
		NoSuchElementException.printStackTrace();
	}
	if(e == null)
	{
		System.out.println("element could not be found, xpath: " + xpath);
	}
	return e;
}
public void scroll(Rectangle rectangle)
{
	int scWidth = getWindowWidth();
	int scHeight = getWindowHeight();
	if(rectangle.x+rectangle.width > scWidth)
	{
		scrollX = rectangle.x+rectangle.width - scWidth;
	}
	else
	{
		scrollX = 0;
	}
	if(rectangle.y+rectangle.height > scHeight)
	{
		scrollY = rectangle.y+rectangle.height - scHeight;
	}
	else
	{
		scrollY = 0;
	}
	int offsetX = 0;
	int offsetY = 0;
	jsExc.executeScript("scroll("+ scrollX +","+ scrollY +");"); //scroll to bottom right point of rectangle into view
	//System.out.println("Scrolled to: " + scrollX +","+ scrollY);
	Object obj = jsExc.executeScript("return window.pageXOffset;");
	if(obj instanceof Long)
	{
		offsetX = Math.toIntExact((Long) obj);
	}
	else if(obj instanceof Double)
	{
		offsetX = ((Double) obj).intValue();
	}
	obj = jsExc.executeScript("return window.pageYOffset;");
	if(obj instanceof Long)
	{
		offsetY = Math.toIntExact((Long) obj);
	}
	else if(obj instanceof Double)
	{
		offsetY = ((Double) obj).intValue();
	}


	
	//System.out.println("Offset   to: " + offsetX +","+ offsetY);
	
	if((scrollX - offsetX) > 0)
	{
		cantReachX = scrollX - offsetX;
	}
	else
	{
		cantReachX = 0;
	}
	if((scrollY - offsetY) > 0)
	{
		cantReachY = scrollY - offsetY;
	}
	else
	{
		cantReachY = 0;
	}
	scrollX = offsetX;
	scrollY = offsetY;
	//Tool.pause("scrolled X: " + scrollX + " Y: " + scrollY + " --- offset X: " + offSetX + " Y: " + offSetY);
	Assist.waitToSettle();
}
public int getWindowHeight()
{
	int heightJS = Math.toIntExact((Long) jsExc.executeScript("return document.documentElement.clientHeight;"));
	int heightWD = driver.manage().window().getSize().height;
	//System.out.println("JS H: " + heightJS + " WD H:" + heightWD);
	return heightJS;
}
public int getWindowWidth()
{
	int widthJS = Math.toIntExact((Long) jsExc.executeScript("return document.documentElement.clientWidth;"));
	int widthWD =  driver.manage().window().getSize().width;
	//System.out.println("JS W: " + widthJS + " WD W:" + widthWD);
	return widthJS;
	
}
public void shutdown()
{
	if(driver != null)
	{
		driver.quit();
	}
	
}
}
