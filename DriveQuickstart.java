import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;


import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;


public class DriveQuickstart {
    private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
	
    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/client_secret.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = DriveQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();	
        //List names and IDs of files in specific folder
        //FileList result = service.files().list()
		//		//application/x-binary for bin files; (application or test)/xml for xml files
		//		.setQ("'1xAHx3vNSubIemliFcMbo27oJrSjNmU0T' in parents " + "and (mimeType='image/jpeg')")
		//		.setFields("nextPageToken, files(id, name)")
        //        .execute();
		
		
		Scanner scanner = new Scanner(System.in);
		String protocol = scanner.nextLine();
		System.out.println("Enter '1' for Upload. Enter '2' for Download. ");	
		switch (protocol) {
				case "1":
					CreateFolder(service, "bin");
				case "2":
					downloadFile(service, "C:/Users/Xc15m/Downloads/Logs");
					downloadFile(service, "C:/Users/Xc15m/Downloads/Logs/bin");
		}
		
		
		//upload file
		//File fileMetadata = new File();
		//fileMetadata.setName("photo.jpg");
		//java.io.File filePath = new java.io.File("C:/Users/Xc15m/Downloads/photo.jpg");
		//FileContent mediaContent = new FileContent("image/jpeg", filePath);
		//File file = service.files().create(fileMetadata, mediaContent)
		//		.setFields("id")
		//		.execute();
		//System.out.println("File ID: " + file.getId());
		
		
		//************test commands***********
		//deleteFile(service, "1Vp8c-mMh4YMBCvMIxn-0Sn3XKdTQfa_W");
		//CreateFolder(service, "bin");
		File folder = new File("C:/Users/Xc15m/Downloads/UploadTestFolder");
		listFilesFromFolder(folder);
		//downloadFile(service, "C:/Users/Xc15m/Downloads/");
    }

	//delete file (need fileID)
	private static void deleteFile(Drive service, String fileId) {
		try {
			service.files().delete(fileId).execute();
		} catch (IOException e) {
			System.out.println("An error occurred: " + e);
		}
	}
	
	//upload file (need fileID)
	//private static void uploadFile(Drive service, String fileID) {
	//	String originfolder = "C:/Users/Xc15m/Downloads";
	//	try {
	//		File dir = new java.io.File(originfolder);
	//		String [] files = dir.list();
	//		for (String file : files) {
	//			System.out.println(file);
	//		}
	//	} catch (IOException e) {
	//		System.out.println("An error occurred: " + e);
	//	}
	//}
	
	//download file (need fileID)
	private static void downloadFile(Drive service, String destinationfolder) {
		try {
			FileList result = service.files().list()
				//application/x-binary for bin files; (application or test)/xml for xml files
				.setQ("'1xAHx3vNSubIemliFcMbo27oJrSjNmU0T' in parents " + "and (mimeType='image/jpeg')")
				.setFields("nextPageToken, files(id, name)")
                .execute();
			List<File> files = result.getFiles();
			//String destinationfolder = "C:/Users/Xc15m/Downloads/";
			if (files == null || files.isEmpty()) {
				System.out.println("No files found.");
			} else {
				System.out.println("Files:");
				for (File file : files) {
					System.out.printf("%s (%s)\n", file.getName(), file.getId());
					String fileId = file.getId();
					//System.out.println(fileId);
					String fileName = file.getName();
					//System.out.println(fileName);
					OutputStream outputstream = new FileOutputStream(destinationfolder + fileName);
					service.files().get(fileId)
					.executeMediaAndDownloadTo(outputstream);
					outputstream.flush();
					outputstream.close();
				}
			}
		} catch (IOException e) {
			System.out.println("An error occurred: " + e);
		}
	}
	
	//create subfolder
	private static String CreateFolder(Drive service, String foldername) {
		try{
		File fileMetadata = new File();
		fileMetadata.setName(foldername);
		fileMetadata.setMimeType("application/vnd.google-apps.folder");
		fileMetadata.setParents(Collections.singletonList("1xAHx3vNSubIemliFcMbo27oJrSjNmU0T"));
		File file = service.files().create(fileMetadata)
			.setFields("id")
			.execute();
		System.out.println("Folder ID: " + file.getId());
		} catch(IOException e) {
			System.out.println("An error occurred: " + e);
		}
		return null;
	}
	
	//get list of files from local folder
	public static void listFilesFromFolder(Drive service, File folder) {
		try {
			for (File fileEntry : folder.listFiles()) {
				if (fileEntry.isDirectory()) {
					listFilesForFolder(fileEntry);
				} else {
					System.out.println(fileEntry.getName());
				}
			}
		} catch(IOException e) {
			System.out.println("An error occurred: " + e);
		}
		return null;
	}
}
