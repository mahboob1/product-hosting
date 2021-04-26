package com.intuit.billingcomm.billing.qbeshostingabt.test;

import com.intuit.billingcomm.billing.qbescommon.enums.EntitledProductFeatureProcessingStateEnum;
import com.intuit.billingcomm.billing.qbescommon.enums.QbesHostingRequestStagingStatusEnum;
import com.intuit.billingcomm.billing.qbeshosting.TestHelpers;
import com.intuit.billingcomm.billing.qbeshosting.jms.SubscriptionEventListener;
import com.intuit.billingcomm.billing.qbeshostingabt.TestApp;
import com.intuit.billingcomm.billing.qbespfts.config.QbesPftsConfig;
import com.intuit.billingcomm.billing.qbespfts.service.SftpConnectionHandler;
import com.intuit.platform.integration.hats.common.OfflineTicketAuthorizationHeader;
import com.intuit.platform.integration.iamticket.client.IAMOfflineTicketClient;
import com.intuit.platform.sdk.ius.service.IusSdk;
import com.intuit.platform.webs.subscription.internal.model.entity.QbesHostingRequestStagingEntity;
import com.intuit.platform.webs.subscription.internal.model.repository.QbesHostingRequestStagingRepository;
import com.intuit.schema.platform.ius.entities.*;
import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApp.class)
@ActiveProfiles("test")
public class SubscriptionEventListenerTest {
    private static final String SFTP_MOCK_FOLDER = "sftp-mock";
    private static final String SFTP_MOCK_HOSTKEY = "hostkey.ser";

    @MockBean
    private IusSdk iusSdk;

    @MockBean
    private IAMOfflineTicketClient iamOfflineTicketClient;

    @Autowired
    private QbesPftsConfig qbesPftsConfig;

    @Autowired
    private SftpConnectionHandler sftpConnectionHandler;

    @Autowired
    private SubscriptionEventListener subscriptionEventListener;

    @Autowired
    private QbesHostingRequestStagingRepository qbesHostingRequestStagingRepository;

    private SshServer sshd;

    @Before
    public void init() throws Exception {
        mockIus();
        setUpSftpServer();
    }

    @After
    public void tearDown() throws Exception {
        tearDownSftpServer();
    }

    @Test
    public void receive_NewSignup() throws Exception {
        subscriptionEventListener.receive(TestHelpers.generateValidSubscriptionMessage());
        QbesHostingRequestStagingEntity req = qbesHostingRequestStagingRepository
                .findByEntitledProductFeatureProcessingEntitledProductFeatureProcessingId(BigInteger.ONE);
        Assert.assertEquals(QbesHostingRequestStagingStatusEnum.SUBMITTED.name(), req.getStatus());
        Assert.assertEquals(EntitledProductFeatureProcessingStateEnum.PENDING.name(),
                req.getEntitledProductFeatureProcessing().getProcessingState());
        Assert.assertTrue(isReqFileInSftp());
    }

    private void setUpSftpServer() throws Exception {
        // Ensure no temporary test files exist
        deleteTemporaryTestFiles();

        // Create folder to server as mock SFTP server directory
        Files.createDirectory(Paths.get(SFTP_MOCK_FOLDER));

        // Pick an available port for the mock SFTP server
        ServerSocket serverSocket = new ServerSocket(0);
        qbesPftsConfig.setPftsPort(serverSocket.getLocalPort());
        serverSocket.close();

        // Set up the mock SFTP server
        sshd = SshServer.setUpDefaultServer();
        VirtualFileSystemFactory fileSystemFactory = new VirtualFileSystemFactory();
        String rootFolder = System.getProperty("user.dir") + "/" + SFTP_MOCK_FOLDER;
        fileSystemFactory.setDefaultHomeDir(Paths.get(rootFolder));
        sshd.setFileSystemFactory(fileSystemFactory);
        sshd.setPort(qbesPftsConfig.getPftsPort());
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(Paths.get(SFTP_MOCK_HOSTKEY)));
        sshd.setPublickeyAuthenticator((s, publicKey, serverSession) -> true);
        sshd.setSubsystemFactories(Collections.singletonList(new SftpSubsystemFactory()));
        sshd.start();
    }

    private void tearDownSftpServer() throws Exception {
        sshd.close();
        deleteTemporaryTestFiles();
    }

    private void deleteTemporaryTestFiles() throws Exception {
        FileUtils.deleteDirectory(new File(SFTP_MOCK_FOLDER));
        Files.deleteIfExists(Paths.get(SFTP_MOCK_HOSTKEY));
    }

    private void mockIus() throws Exception {
        Mockito.when(iamOfflineTicketClient.getAuthHeaderForSystemOfflineTicket(Mockito.any())).thenReturn(
                new OfflineTicketAuthorizationHeader.Builder()
                        .setAppId("test")
                        .setAppSecret("test")
                        .setToken("test")
                        .build()
        );
        Mockito.when(iusSdk.getRealmByRealmIdAsync(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(
                CompletableFuture.completedFuture(new Realm() {{
                    setDisplayName(new ArrayList<String>() {{ add(getTestCompanyName()); }});
                    setAddress(new ArrayList<Address>() {{ add(getTestAddress()); }});
                }})
        );
        Mockito.when(iusSdk.getMasterAdminPersonaAsync(Mockito.any(), Mockito.any())).thenReturn(
                CompletableFuture.completedFuture(new Persona() {{
                    setFullName(new ArrayList<FullName>() {{ add(getTestFullName()); }});
                    setPhone(new ArrayList<Phone>() {{ add(getTestPhone()); }});
                }})
        );
    }

    private Address getTestAddress() {
        return new Address() {{
            setPostalCode("PostalCode");
            setStateOrProvince("Province");
            setCityOrLocality("City");
        }};
    }

    private String getTestCompanyName() {
        return "Test Company";
    }

    private FullName getTestFullName() {
        return new FullName() {{
            setGivenName("First");
            setMiddleName("Middle");
            setSurName("Last");
        }};
    }

    private Phone getTestPhone() {
        return new Phone() {{
            setPhoneNumber("+1 780 111 2222");
        }};
    }

    private boolean isReqFileInSftp() throws Exception {
        ChannelSftp channelSftp = sftpConnectionHandler.sftpConnect();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Edmonton"));
        String reqFileNameFormat = "TRF-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HH")) + "*.xml";
        boolean isFound = Collections.list(
                channelSftp.ls(qbesPftsConfig.getPftsResFolder() + reqFileNameFormat).elements()).size() > 0;
        sftpConnectionHandler.sftpDisconnect(channelSftp);
        return isFound;
    }
}
