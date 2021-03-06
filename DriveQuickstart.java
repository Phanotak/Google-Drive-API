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
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.client.http.FileContent;

import java.io.*;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.*;


public class DriveQuickstart {
	private static final String APPLICATION_NAME = "Google Drive API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
	private static final String CREDENTIALS_FILE_PATH = "src/main/resources/client_secret.json";
	//private static final String CREDENTIALS_FILE_PATH = "C:/Quickstart/src/main/resources/client_secret.json";
	
	/**
	 * Creates an authorized Credential object.
	 * 
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		// InputStream in =
		// zDriveQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
		if (in == null) {
			throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
		}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	public static void main(String... args) throws IOException, GeneralSecurityException {
			// Build a new authorized API client service.
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME).build();
		Console console = System.console();
		if (args.length < 3) {
			console.writer().println(
					"Format to run programs is as follows: [java] [name of program] [parameter1] [parameter2] [parameter3]");
		}

		String direction = args[0];
		String src_dir = args[1];
		String dst_dir = args[2];
		String date;
		if (args.length == 3)
		{
			date = new SimpleDateFormat("yyyyMMdd").format(new Date());
		}
		else
		{
			date = args[3];
		}
		//String mime_type = args[3];
		String upld = "0";
		String dwnld = "1";
		
		if (direction.equals(upld)) {
			String uploadfoldername = dst_dir.concat("." + date);
			Files.List request = service.files().list()
				.setQ("mimeType='application/vnd.google-apps.folder' and trashed=false and 'root' in parents and name contains 'Log.'");
			FileList files = request.execute();
			System.out.println(files);
			if (files.isEmpty() != true) {
			 	CreateFolder(service, uploadfoldername, null);
			}
			//get folder id of destination folder on Google Drive
			String folder_id = getGoogleSubFolderID(null, uploadfoldername);
			uploadFile(service, src_dir, folder_id);
			CreateFolder(service, "bin", folder_id);
			String bin_folder_id = getGoogleSubFolderByName(folder_id, "bin");
			uploadFile(service, src_dir.concat("bin/"), bin_folder_id);
		} else if (direction.equals(dwnld)) {
			String market_xml = "market_" + date + ".xml";
			String indices_xml = "indices_" + date + ".xml";
			String src_folder_id = getGoogleSubFolderID(null, src_dir);
			String bin_path = dst_dir.concat("bin/");
			//String src_folder_id_clean = src_folder_id.replaceAll("[_-]", "");
			String bin_folder_id = getGoogleSubFolderByName(src_folder_id, "bin");
			String market_xml_id = getGoogleSubFileID(src_folder_id, market_xml);
			System.out.println(market_xml_id);
			String indices_xml_id = getGoogleSubFileID(src_folder_id, indices_xml);
			System.out.println(indices_xml_id);
			List<File> bin_files = getbinfolderlist(service, bin_folder_id);
			System.out.println(bin_files.size());
			
			Path dstpath = Paths.get(dst_dir);
			if (!java.nio.file.Files.exists(dstpath)) {
				try {
					java.nio.file.Files.createDirectories(dstpath);
				} catch (IOException e) {
					//fail to create directory
					e.printStackTrace();
				}
			}
			Path binpath = Paths.get(bin_path);
			if (!java.nio.file.Files.exists(binpath)) {
				try {
					java.nio.file.Files.createDirectories(binpath);
				} catch (IOException e) {
					//fail to create directory
					e.printStackTrace();
				}
			}
			downloadFile(service, indices_xml, indices_xml_id, dst_dir);
			downloadFile(service, market_xml, market_xml_id, dst_dir);
			downloadBinFiles(service, bin_files, bin_path);
		}
    }
	
	
	//upload file
	private static void uploadFile(Drive service, String src_dir, String dst_dir) throws IOException {
			java.io.File dir = new java.io.File(src_dir);
			java.io.File [] fileslist = dir.listFiles(new FileFilter() {
				public boolean accept(java.io.File pathname) {
					String name = pathname.getName().toLowerCase();
					return name.endsWith(".xml") || name.endsWith(".bin") && pathname.isFile();
				}
			});
			String[] names = new String[fileslist.length];
			for (int i = 0; i < fileslist.length; i++) {
			   names[i] = fileslist[i].getName();
			}
			for (String file : names) {
				File uploadfile = new File();
				uploadfile.setName(file);
				uploadfile.setParents(Collections.singletonList(dst_dir));
				java.io.File filePath = new java.io.File(src_dir + file);
				Path path = filePath.toPath();
				String mime_type = java.nio.file.Files.probeContentType(path);
				FileContent mediaContent = new FileContent(mime_type, filePath);
				File uploadedfile = service.files().create(uploadfile, mediaContent)
					.setFields("id")
					.execute();
				System.out.println("File ID: " + uploadedfile);
			}				
	}
	
	//download file (need fileID)
	private static void downloadFile(Drive service, String fileName, String fileId, String dst_dir) throws IOException {
		service = GoogleDriveUtils.getDriveService();
		OutputStream outputstream = new FileOutputStream(dst_dir + fileName);
		service.files().get(fileId)
			.executeMediaAndDownloadTo(outputstream);
		outputstream.flush();
		outputstream.close();
	}
	
	public static void downloadBinFiles(Drive service, List<File> files, String dst_dir) throws IOException{
		int i = 0;
		for (File file : files) {
			try {
				String fileId = file.getId();
				String fileName = file.getName();
				//need to check if folder exists. If not, create folder.
				System.out.println("File downloading is: " + fileName);
				OutputStream outputstream = new FileOutputStream(dst_dir + fileName);
					service.files().get(fileId)
						.executeMediaAndDownloadTo(outputstream);
					outputstream.flush();
					outputstream.close();
				i++;
				System.out.println(i + " files downloaded.");
			} catch (Exception ignore) {
				i++;
				System.out.println(i + " files downloaded.");
				continue;
			}
			// } catch (IOException e) {
			// 	Console console = System.console();
			// 	console.writer().println("Error.");
			// 	e.getMessage();
			// 	e.printStackTrace();	
			// }
		}
	}

	public static final List<File> getbinfolderlist(Drive service, String bin_folder_id) throws IOException {
		try {
			List<File> list = new ArrayList<File>();
			String pageToken = null;
			do {
				FileList result = service.files().list()
					.setQ("'" + bin_folder_id + "' in parents")
					.setPageToken(pageToken)
					.execute();
					for (File file : result.getFiles()) {
						list.add(file);
					}
					pageToken = result.getNextPageToken();
				} while (pageToken != null);
				return list;
		} catch (Exception e) {
			return null;
		}
	}

	public static final List<File> getGoogleSubFolders(String googleFolderIdParent) throws IOException {
        Drive driveService = GoogleDriveUtils.getDriveService();
        String pageToken = null;
        List<File> list = new ArrayList<File>();
        String query = null;
        if (googleFolderIdParent == null) {
            query = " mimeType = 'application/vnd.google-apps.folder' " //
                    + " and 'root' in parents";
        } else {
            query = " mimeType = 'application/vnd.google-apps.folder' " //
                    + " and '" + googleFolderIdParent + "' in parents";
        }
 
        do {
            FileList result = driveService.files().list().setQ(query).setSpaces("drive") //
                    // Fields will be assigned values: id, name, createdTime
                    .setFields("nextPageToken, files(id, name, createdTime)")//
					.setPageToken(pageToken)
					.execute();
            for (File file : result.getFiles()) {
                list.add(file);
            }
            pageToken = result.getNextPageToken();
        } while (pageToken != null);
        //
        return list;
    }

	public static final String getGoogleSubFolderID(String googleFolderIdParent, String dir) throws IOException {
 
		try {
			Drive driveService = GoogleDriveUtils.getDriveService();
			String pageToken = null;
			List<File> list = new ArrayList<File>();
			String query = null;
			if (googleFolderIdParent == null) {
				query = " mimeType = 'application/vnd.google-apps.folder' " //
						+ " and 'root' in parents";
			} else {
				query = " mimeType = 'application/vnd.google-apps.folder' " //
						+ " and '" + googleFolderIdParent + "' in parents";
			}
	
			do {
				FileList result = driveService.files().list()
						.setQ(query)
						.setSpaces("drive")
						// Fields will be assigned values: id, name, createdTime
						.setFields("nextPageToken, files(id, name, createdTime)")
						.setPageToken(pageToken).execute();
				for (File folderid : result.getFiles()) {
					list.add(folderid);
				}
				pageToken = result.getNextPageToken();
			} while (pageToken != null);
			
			//return list;
			for (File folder : list)
			{
				// if (file.getMimeType() = application/vnd.google-apps.folder)
				// 	continue;
				String name1 = folder.getName().toLowerCase();
				String name2 = dir.toLowerCase();
				if (name1.toLowerCase().equals(name2))
				{
					return folder.getId();
				}
			}
			return "";
		}
		catch(Exception e) {
			return "";
		}
    }

	public static final String getGoogleSubFileID(String src_folder_id, String filename) throws IOException {
		try {
			String pageToken = null;
			List<File> list = new ArrayList<File>();
			Drive driveService = GoogleDriveUtils.getDriveService();
			do {
				FileList result = driveService.files().list()
					.setQ("'" + src_folder_id + "' in parents")
					.setSpaces("drive")
					.setFields("nextPageToken, files(id, name, createdTime)")
					.setPageToken(pageToken)
					.execute();
				List<File> files = result.getFiles();
				if (files == null || files.isEmpty()) {
					System.out.println("No files found.");
				} else {
					System.out.println("There are files");
					for (File file : result.getFiles()) {
						list.add(file);
					}
				}
				pageToken = result.getNextPageToken();
				for (File file : list) {
					if (filename.equals(file.getName()))
					{
						return file.getId();
					}
			}
			return "";
			} while (pageToken != null);
		} catch (Exception e) {
			return "";
		}
	}

	public static final List<File> getGoogleRootFolders() throws IOException {
        return getGoogleSubFolders(null);
    }

	public static final String getGoogleSubFolderByName(String googleFolderIdParent, String subFolderName) throws IOException {
		try {
			Drive driveService = GoogleDriveUtils.getDriveService();
	
			String pageToken = null;
			List<File> list = new ArrayList<File>();
	
			String query = null;
			if (googleFolderIdParent == null) {
				query = " name = '" + subFolderName + "' " 
						+ " and mimeType = 'application/vnd.google-apps.folder' " 
						+ " and 'root' in parents";
			} else {
				query = " name = '" + subFolderName + "' " 
						+ " and mimeType = 'application/vnd.google-apps.folder' " 
						+ " and '" + googleFolderIdParent + "' in parents";
			}
	
			do {
				FileList result = driveService.files().list()
						.setQ(query)
						.setSpaces("drive") 
						.setFields("nextPageToken, files(id, name, createdTime)")
						.setPageToken(pageToken).execute();
				for (File file : result.getFiles()) {
					list.add(file);
					
				}
				pageToken = result.getNextPageToken();
			} while (pageToken != null);
			for (File file : list) {
				return file.getId();
			}
			return "";
		} catch (Exception e) {
			return "";
		}
    }

	//first value needs to be id (string) of parent folder. Enter name of subfolder in main method
	public static final String getGoogleRootFoldersByName(String subFolderName) throws IOException {
        return getGoogleSubFolderByName(null, subFolderName);
    }
	
	//create subfolder
	private static String CreateFolder(Drive service, String foldername, String parent_folder_id) {
		try{
		File fileMetadata = new File();
		fileMetadata.setName(foldername);
		fileMetadata.setMimeType("application/vnd.google-apps.folder");
		fileMetadata.setParents(Collections.singletonList(parent_folder_id));
		File file = service.files().create(fileMetadata)
			.setFields("id")
			.execute();
		System.out.println("Folder ID: " + file.getId());
		} catch(IOException e) {
			System.out.println("An error occurred: " + e);
		}
		return null;
	}
	/*
	//delete file (need fileID)
	private static void deleteFile(Drive service, String fileId) {
		try {
			service.files().delete(fileId).execute();
		} catch (IOException e) {
			System.out.println("An error occurred: " + e);
		}
	}
	*/
}
