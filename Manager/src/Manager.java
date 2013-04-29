import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;

public class Manager {
	private String[] imageUrls;
	private int numOfWorkers;
	private int numOfMessage;
	private int numOfJobsForWorker;
	private PropertiesCredentials Pc;
	private AWSCredentials Credentials;
	private AmazonS3 S3;
	private String keyBucketName;
	private String BucketName;
	private AmazonSQS AmazonSQS;

	public Manager() {
		try {
			Pc = new PropertiesCredentials(
					Manager.class
							.getResourceAsStream("AwsCredentials.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Credentials = Pc;
		S3 = new AmazonS3Client(Credentials);
		AmazonSQS = new AmazonSQSClient(Pc);

	}

	public void getOrderFromLocal() {
		System.out.println("Receiving orders from local.\n");

		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(
				ConstantProvider.LOCAL_TO_MANAGER_QUEUE);
		receiveMessageRequest.setMaxNumberOfMessages(2);
		List<Message> messages = AmazonSQS
				.receiveMessage(receiveMessageRequest).getMessages();
		String messegeSplit[] = messages.get(0).getBody().split(" ");
		BucketName = messegeSplit[0];
		keyBucketName = messegeSplit[1];

		System.out.println("Downloading File from server");
		S3Object object = S3.getObject(new GetObjectRequest(BucketName,
				keyBucketName));
		System.out.println("Content-Type: "
				+ object.getObjectMetadata().getContentType());
		try {
			imageUrls = parseInputStream(object.getObjectContent());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		numOfMessage = imageUrls.length - 1;
		// Delete the messege
		// System.out.println("Deleting order messege.\n");
		// String messageRecieptHandle = messages.get(0).getReceiptHandle();
		// AmazonSQS.deleteMessage(new DeleteMessageRequest(
		// ConstantProvider.LOCAL_TO_MANAGER_QUEUE, messageRecieptHandle));
		// S3.deleteObject(BucketName, keyBucketName);

	}

	private void setJobsForWorker(String jobs) {
		numOfJobsForWorker = Integer.parseInt(jobs);

	}

	private void setWorkersForJobs() {
		numOfWorkers = numOfMessage / numOfJobsForWorker;
		if (numOfMessage % numOfJobsForWorker != 0)
			numOfWorkers++;

	}

	private void uploadMesseagesToQueue() {
		for (int i = 1; i < imageUrls.length; i++) {
			AmazonSQS.sendMessage(new SendMessageRequest(
					ConstantProvider.MESSEAGES_QUEUE, imageUrls[i]));
		}

	}

	private String[] parseInputStream(InputStream input) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		String ImageUrls = "";
		while (true) {
			String line = reader.readLine();
			if (line == null)
				break;
			ImageUrls = ImageUrls + "\n" + line;

		}
		String[] ans = ImageUrls.split("\n");

		return ans;
	}

	public static void main(String[] args) throws Exception {
		Manager manager = new Manager();
		manager.getOrderFromLocal();
		manager.setJobsForWorker(args[1]);
		manager.setWorkersForJobs();
		manager.uploadMesseagesToQueue();
		// TODO : check why listing?

	}

}
