package guru.springframework.controllers;

import guru.springframework.domain.Product;
import guru.springframework.services.ProductService;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.Summary;
//import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.exporter.common.TextFormat;

import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.sun.net.httpserver.HttpServer;

@Controller
public class ProductController {

    private ProductService productService;

    private static double rand(double min, double max) {
        return min + (Math.random() * (max - min));
    }
    
    public static double number = 1;
    public static Counter ProductRequestsTotal = Counter.build().namespace("java").name("my_counter").help("Total requests.").register();    
    public static Gauge gauge = Gauge.build().namespace("java").name("my_gauge").help("This is my gauge").register();
    public static Histogram histogram = Histogram.build().namespace("java").name("my_histogram").help("This is my histogram").register();
    public static Summary summary = Summary.build().namespace("java").name("my_summary").help("This is my summary").register();
    public static Histogram requestHistogram = Histogram.build().namespace("java").name("request").help("Requets histogram").labelNames("statusCode").register();
        
    @Autowired
    public void setProductService(ProductService productService) {
        this.productService = productService;
    }

    @RequestMapping(value = "/products", method = RequestMethod.GET)
    public String list(Model model){
    	//ProductRequestsTotal.inc();
    	 int statusCode = 200;
    	 if(number%3==0) {
    		statusCode = 500;
    	 }
    	 Date start = new Date();
         long elapsedTime = (new Date()).getTime() - start.getTime();
    	try {       
	            model.addAttribute("products", productService.listAllProducts());
	            System.out.println("Returning rpoducts:");
	            
            } catch (Exception e) {
        	   statusCode=500;
        	   e.printStackTrace();
        }
    	finally {
    		requestHistogram.labels(String.valueOf(statusCode)).observe(elapsedTime);
    		ProductRequestsTotal.inc();
            gauge.set(rand(-5, 10));
            histogram.observe(rand(0, 5));
            summary.observe(rand(0, 5));
            number++;
    	}
       
        return "products";
    }

    @RequestMapping("product/{id}")
    public String showProduct(@PathVariable Integer id, Model model){
        model.addAttribute("product", productService.getProductById(id));
        return "productshow";
    }

    @RequestMapping("product/edit/{id}")
    public String edit(@PathVariable Integer id, Model model){
        model.addAttribute("product", productService.getProductById(id));
        return "productform";
    }

    @RequestMapping("product/new")
    public String newProduct(Model model){
    	//newProductController.ProductRequestsNew.inc();
        model.addAttribute("product", new Product());
        return "productform";
    }

    @RequestMapping(value = "product", method = RequestMethod.POST)
    public String saveProduct(Product product){

        productService.saveProduct(product);

        return "redirect:/product/" + product.getId();
    }
    
    @RequestMapping(path = "/product-metric")
    public void metrics(Writer responseWriter) throws IOException {
        TextFormat.write004(responseWriter, CollectorRegistry.defaultRegistry.metricFamilySamples());
        responseWriter.close();
    }

}
