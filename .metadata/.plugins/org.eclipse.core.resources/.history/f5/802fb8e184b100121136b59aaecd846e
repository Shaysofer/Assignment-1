import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.asprise.util.ocr.OCR ;

public class Worker {

	public static void main(String args[]) throws Exception{
		AmazonSQS sqs = new AmazonSQSClient(new PropertiesCredentials(
				Worker.class.getResourceAsStream("AwsCredentials.properties")));
		List<String> l = sqs.listQueues().getQueueUrls();
		String url  =l.get(0);
		String url2 =l.get(1);
		ReceiveMessageRequest recieve = new ReceiveMessageRequest(url);
		recieve.setVisibilityTimeout(5);
		List<Message> messages = sqs.receiveMessage(recieve).getMessages();
		while (messages.size() != 0){
			
			String mas = "";
			for (Message message : messages) {
				mas = message.getBody();
			}
			URL url1 = new URL(mas);
			BufferedImage image = ImageIO.read(url1);
			String s = new OCR().recognizeEverything(image);
			sqs.sendMessage(new SendMessageRequest(url2,s));
			String messageRecieptHandle = messages.get(0).getReceiptHandle();
			sqs.deleteMessage(new DeleteMessageRequest(url, messageRecieptHandle));
			messages = sqs.receiveMessage(recieve).getMessages();
		}
	}

}
