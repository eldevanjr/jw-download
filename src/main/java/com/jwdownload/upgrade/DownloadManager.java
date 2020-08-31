package com.jwdownload.upgrade;

import com.euneju.Files;
import com.euneju.Root;
import com.euneju.Subcategories;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.ChoiceDialog;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// The Download Manager.
@Data
public class DownloadManager extends JFrame
        implements Observer {

    protected static final Logger logger = LoggerFactory.getLogger(DownloadManager.class);

    private LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime startTimeDownload = LocalDateTime.now();

    private boolean downloadImporta = false;

    private boolean closeOption, hasException = false;

    private boolean closeOptionAlreadyOpen = false;

    // Add download text field.
    private JTextField addTextField;

    private String folderToSave;

    private int urlVerified = 0;

    List<Files> filesToDownload = new ArrayList<>();

    // Download table's data model.
    private DownloadsTableModel tableModel;

    // Table listing downloads.
    private JTable table;

    JFXPanel jfxPanel;

    // These are the buttons for managing the selected download.
    private JButton pauseButton, resumeButton;
    private JButton cancelButton, clearButton, startAllButton, startButton, novaCategory;

    private JProgressBar totalBar;

    JLabel speed = new JLabel("");
    JLabel elapsed = new JLabel("");

    private int totalSize; // size of downloads in bytes
    private int totalDownloaded; // number of bytes downloaded



    // Currently selected download.
    private Download selectedDownload;

    // Flag for whether or not table selection is being cleared.
    private boolean clearing;

    private List<URL> urlList = new ArrayList<>();

    private List<Download> downloads = new ArrayList<>();

    float[] columnWidthPercentage = {40.0f, 9.0f, 38.0f, 13.0f};


    private void resizeColumns() {
        int tW = table.getWidth();
        TableColumn column;
        TableColumnModel jTableColumnModel = table.getColumnModel();
        int cantCols = jTableColumnModel.getColumnCount();
        for (int i = 0; i < cantCols; i++) {
            column = jTableColumnModel.getColumn(i);
            int pWidth = Math.round(columnWidthPercentage[i] * tW);
            column.setPreferredWidth(pWidth);
        }
    }

    public DownloadManager(String folderToSave)  {
        this(folderToSave, false, new ArrayList<>());
    }

    // Constructor for Download Manager.
    public DownloadManager(String folderToSave, boolean downloadImporta, List<Files> filesToDownload) {
        setFolderToSave(folderToSave+(downloadImporta?"/imp":""));
        setDownloadImporta(downloadImporta);
        setFilesToDownload(filesToDownload);
        // Set application title.
        setTitle("Gerenciador de Download JW.ORG");

        // Set window size.
        setSize(800, 480);

//        // Handle window closing events.
//        addWindowListener(new WindowAdapter() {
//            public void windowClosing(WindowEvent e) {
//                actionExit();
//            }
//        });

        // Set up file menu.
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Arquivo");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem fileExitMenuItem = new JMenuItem("Sair",
                KeyEvent.VK_X);
        fileExitMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionExit();
            }
        });
        fileMenu.add(fileExitMenuItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // Set up add panel.
        JPanel addPanel = new JPanel();
        JLabel jLabel = new JLabel("Progresso do Download:");
        addPanel.add(jLabel);
        addPanel.add(speed);
        addPanel.add(elapsed);

        totalBar = new JProgressBar(0,100);
        totalBar.setStringPainted(true);
        addPanel.add(totalBar);

        // Set up Downloads table.
        tableModel = new DownloadsTableModel();
        table = new JTable(tableModel);
        resizeColumns();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeColumns();
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            tableSelectionChanged();
        });
        // Allow only one row at a time to be selected.
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set up ProgressBar as renderer for progress column.
        ProgressRenderer renderer = new ProgressRenderer(0, 100);
        renderer.setStringPainted(true); // show progress text
        table.setDefaultRenderer(JProgressBar.class, renderer);

        // Set table's row height large enough to fit JProgressBar.
        table.setRowHeight(
                (int) renderer.getPreferredSize().getHeight());

        // Set up downloads panel.
        JPanel downloadsPanel = new JPanel();
        downloadsPanel.setBorder(
                BorderFactory.createTitledBorder("Downloads"));
        downloadsPanel.setLayout(new BorderLayout());
        downloadsPanel.add(new JScrollPane(table),
                BorderLayout.CENTER);

        // Set up buttons panel.
        JPanel buttonsPanel = new JPanel();


        startAllButton = new JButton("Baixar Todos");
        startAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionStart();
            }
        });
        startAllButton.setEnabled(true);
        buttonsPanel.add(startAllButton);

        startButton = new JButton("Baixar ");
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionDownload();
            }
        });
        startButton.setEnabled(false);
        buttonsPanel.add(startButton);

        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionPause();
            }
        });
        pauseButton.setEnabled(false);
        buttonsPanel.add(pauseButton);
        resumeButton = new JButton("Continuar");
        resumeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionResume();
            }
        });
        resumeButton.setEnabled(true);
        buttonsPanel.add(resumeButton);
        cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionCancel();
            }
        });
        cancelButton.setEnabled(false);
        buttonsPanel.add(cancelButton);

        novaCategory = new JButton("Escolher nova categoria");
        novaCategory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                      actionExit();
                    }
                });


                callJw();
            }
        });
        novaCategory.setEnabled(true);
        buttonsPanel.add(novaCategory);

        // Add panels to display.
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(addPanel, BorderLayout.NORTH);
        getContentPane().add(downloadsPanel, BorderLayout.CENTER);
        getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    // Exit this program.
    private void actionExit() {
        try {
            this.dispose();
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    // Verify download URL.
    private static URL verifyUrl(String url) {
        // Only allow HTTP URLs.
        if (!url.toLowerCase().startsWith("http://") && !url.toLowerCase().startsWith("https://") ){
            return null;
        }

        // Verify format of URL.
        URL verifiedUrl = null;
        try {
            verifiedUrl = new URL(url);
        } catch (Exception e) {
            return null;
        }

        // Make sure URL specifies a file.
        if (verifiedUrl.getFile().length() < 2)
            return null;

        return verifiedUrl;
    }

    // Called when table row selection changes.
    private void tableSelectionChanged() {
    /* Unregister from receiving notifications
       from the last selected download. */
        if (selectedDownload != null)
            selectedDownload.deleteObserver(DownloadManager.this);
         
    /* If not in the middle of clearing a download,
       set the selected download and register to
       receive notifications from it. */
        if (!clearing) {
            selectedDownload =
                    tableModel.getDownload(table.getSelectedRow());
            selectedDownload.addObserver(DownloadManager.this);
            updateButtons();
        }
    }

    // Resume the selected download.
    private void actionStart() {
        ScheduledExecutorService scheduler2 = Executors.newScheduledThreadPool(1);
        Date date = new Date();
        setStartTimeDownload(LocalDateTime.now());
        scheduler2.scheduleAtFixedRate(()->{
            Date date1 = new Date();
            float tempo = (float) ((float) (date1.getTime()-date.getTime()) / 1000.0);
            String speed = DownloadsTableModel.getStringSizeLengthFile((long) (getTotalDownloaded()/tempo)) + "/ Seg.";
            getSpeed().setText(speed);
//            ProgressDemo.printProgress(getStartTimeDownload().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),100,(long)getProgress());
            if (getProgress()==100){
                getSpeed().setText("");
                scheduler2.shutdownNow();
            }
            long eta = getProgress() == 0 ? 0 :
                    (long) ((100 - getProgress()) * (System.currentTimeMillis() - date.getTime()) / getProgress());
            System.out.println(eta);
            String etaHms = getProgress() == 0 ? "N/A" :
                    String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
                            TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));
            System.out.println(etaHms);
            getElapsed().setText("Decorr:" + getTempoEntreDatas2(getStartTimeDownload(), LocalDateTime.now())+ " - Rest: " +  etaHms);
        }, 5, 985, TimeUnit.MILLISECONDS);
        downloads.sort(Comparator.comparing(Download::getSize));
        downloads.forEach(download -> {
            try {
                download.download();
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        updateButtons();

    }

    // Pause the selected download.
    private void actionDownload() {
        selectedDownload.download();
        updateButtons();
    }

    // Pause the selected download.
    private void actionPause() {
        selectedDownload.pause();
        updateButtons();
    }

    // Resume the selected download.
    private void actionResume() {
        selectedDownload.resume();
        updateButtons();
    }

    // Cancel the selected download.
    private void actionCancel() {
        selectedDownload.cancel();
        updateButtons();
    }


    /* Update each button's state based off of the
       currently selected download's status. */
    private void updateButtons() {
        if (selectedDownload != null) {
            STATUS_DOWNLOAD status = selectedDownload.getStatus();
            switch (status) {
                case DOWNLOADING:
                    startAllButton.setEnabled(false);
                    startButton.setEnabled(false);
                    pauseButton.setEnabled(true);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    break;
                case PAUSED:
                    startAllButton.setEnabled(true);
                    startButton.setEnabled(true);
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(true);
                    break;
                case ERROR:
                    startAllButton.setEnabled(true);
                    startButton.setEnabled(true);
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    break;
                case NOT_DOWNLOADED:
                    startAllButton.setEnabled(true);
                    startButton.setEnabled(true);
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
                    break;
                default: // COMPLETE or CANCELLED
                    startAllButton.setEnabled(notDownloadedSize()>0);
                    startButton.setEnabled(false);
                    pauseButton.setEnabled(false);
                    resumeButton.setEnabled(false);
                    cancelButton.setEnabled(false);
            }
        } else {
            // No download is selected in table.

            startAllButton.setEnabled(notDownloadedSize()>0);
            startButton.setEnabled(false);
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(false);
            cancelButton.setEnabled(false);
        }
    }

    private int notDownloadedSize(){
        return   downloads.parallelStream().filter(download -> !download.getStatus().equals(STATUS_DOWNLOAD.UPDATED)
                || !download.getStatus().equals(STATUS_DOWNLOAD.COMPLETE)).collect(Collectors.toList()).size();
    }

    /* Update is called when a Download notifies its
       observers of any changes. */
    public void update(Observable o, Object arg) {
        // Update buttons if the selected download has changed.
//        if (selectedDownload != null && selectedDownload.equals(o))
        updateButtons();

    }

    // Get this download's progress.
    public float getProgress() {
        return ((float) getTotalDownloaded() / getTotalSize()) * 100;
    }

    // Run the Download Manager.
    // para rodar background: java -jar jkaw-upgrade-1.0-SNAPSHOT-jar-with-dependencies.jar horas:24 automatic:true background:true
    public static void main(String[] args) throws IOException {
       DownloadManager downloadManager = new DownloadManager("");
       downloadManager.callJw();
    }

    public void callJw() {
        jfxPanel = new JFXPanel();

        Platform.runLater(()->{


//            for(String s : Arrays.asList(args)) {
//                if(s.contains("horas")){
//                    horasEntreAtualizacao = resolveAnyException(() -> Integer.valueOf(s)).orElse(horasEntreAtualizacao);
//                }else if(s.contains("automatic")){
//                    inicioDownloadAutomatico = resolveAnyException(() -> s.split(":")[1].equals("true")).orElse(inicioDownloadAutomatico);
//                }else if(s.contains("background")){
//                    inicioBackgound = resolveAnyException(() -> s.split(":")[1].equals("true")).orElse(inicioBackgound);
//                }else if(s.contains("importa:")){
//
//                    downloadImporta = resolveAnyException(() -> s.split(":")[1].equals("true")).orElse(downloadImporta);
//                    System.out.println("downloadImporta>>" + downloadImporta);
//                }
//            }
            ObjectMapper objectMapper = new ObjectMapper();
// configure your ObjectMapper here
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            RestTemplate restTemplate = new RestTemplate();

            MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
            messageConverter.setPrettyPrint(false);
            messageConverter.setObjectMapper(objectMapper);
            restTemplate.getMessageConverters().removeIf(m -> m.getClass().getName().equals(MappingJackson2HttpMessageConverter.class.getName()));
            restTemplate.getMessageConverters().add(messageConverter);
            String url = "https://data.jw-api.org/mediator/v1/categories/T/";
            Root root = restTemplate.getForObject(url+"VideoOnDemand?detailed=1", Root.class);
            List<Files> filesToDownload = new ArrayList<>();
            Long sizeFile = 0L;
            root.getCategory().getSubcategories().forEach(System.out::println);
            Platform.setImplicitExit(false);
            ChoiceDialog<Subcategories> dialog = new ChoiceDialog();

            dialog.getItems().addAll(root.getCategory().getSubcategories());
            dialog.setTitle("Escolha a Categoria");
            dialog.setHeaderText("Escolha a Categoria");

// Traditional way to get the response value.
            Subcategories result = dialog.showAndWait().orElse(root.getCategory().getSubcategories().get(0));
            Platform.runLater(() -> {
                System.out.println(result.getName());
                System.out.println(result.getKey());
                Root root2 = restTemplate.getForObject(url+result.getKey()+"?detailed=1", Root.class);
                ChoiceDialog<Subcategories> dialog2 = new ChoiceDialog();

                dialog2.getItems().addAll(root2.getCategory().getSubcategories());
                dialog2.setTitle("Escolha a Sub-Categoria");
                dialog2.setHeaderText("Escolha a Categoria");
                ChoiceDialog<String> dialog3 = new ChoiceDialog();
                dialog3.getItems().addAll(Arrays.asList("240", "360", "480", "720"));

                dialog3.setTitle("Escolha a Qualidade a ser baixada");
                dialog3.setHeaderText("Escolha a Qualidade:");
                String qld = dialog3.showAndWait().orElse("480");

// Traditional way to get the response value.
                Subcategories result2 = dialog2.showAndWait().orElse(root2.getCategory().getSubcategories().get(0));
                root2.getCategory().getSubcategories().parallelStream().filter(subcategories1 -> !subcategories1.getKey().contains("Featured"))   .filter(subcategories2 ->  subcategories2.equals(result2)).forEach(subcategories1 -> {
                       String folderToSave = normalizeName(result.getName()) + "/" + normalizeName(subcategories1.getName());


                       subcategories1.getMedia().forEach(media -> {

                           Optional<Files> toSave = Optional.empty();
                           if(media.getFiles().stream()
                                   .anyMatch(files -> files.getLabel().contains(qld+"p"))){
                               toSave = media.getFiles().stream().filter(files -> files.getLabel().contains(qld+"p") && !files.isSubtitled()).findFirst();
                           }else if(media.getFiles().stream()
                                   .anyMatch(files -> files.getLabel().contains("720p") && files.getFilesize()<=524290000)){
                               toSave = media.getFiles().stream().filter(files -> files.getLabel().contains("720p") && !files.isSubtitled()).findFirst();
                           }else if(media.getFiles().stream().anyMatch(files -> files.getLabel().contains("480p") && files.getFilesize()<=524290000)){
                               toSave = media.getFiles().stream().filter(files -> files.getLabel().contains("480p") && !files.isSubtitled()).findFirst();
                           }else if(media.getFiles().stream().anyMatch(files -> files.getLabel().contains("360p"))){
                               toSave = media.getFiles().stream().filter(files -> files.getLabel().contains("360p") && !files.isSubtitled()).findFirst();
                           }else if(media.getFiles().stream().anyMatch(files -> files.getLabel().contains("240p"))){
                               toSave = media.getFiles().stream().filter(files -> files.getLabel().contains("240p") && !files.isSubtitled()).findFirst();
                           }else{
                               toSave = media.getFiles().stream().findAny();
                           }
                           //menor que 500MB
                           toSave.get().setNameToSave(folderToSave +
                                   "/"+normalizeName(media.getTitle().replaceAll("\\.", "")+"_"
                                   +toSave.get().getLabel())+(media.getFiles().stream().findAny().get().getMimetype().contains("mp4")?".mp4":""));
                           filesToDownload.add(toSave.get());
                           System.out.println(toSave.get().getProgressiveDownloadURL());
                   });
                });


                root.getCategory().getSubcategories().parallelStream()

                        .filter(subcategories ->  subcategories.equals(result))
                        .forEach(subcategories -> {

                        });


                System.out.println(DownloadsTableModel.getStringSizeLengthFile(filesToDownload.stream().mapToLong(value -> value.getFilesize()).reduce(0, Long::sum)));

                try {
                    callUpgrade(new DownloadManager(System.getProperty("user.home")+"/jw-download",
                                    false , filesToDownload),
                            24, false, false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        });
    }

    public static String normalizeName(String name){
        return  unaccent(name.trim()).replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    public static String unaccent(String src) {
        return Normalizer
                .normalize(src, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
    }

    public static  void callUpgrade(DownloadManager manager) throws IOException {
        callUpgrade(manager, 24, false, false);
    }

    public static  void callUpgrade(DownloadManager manager, int horasEntreAtualizacao) throws IOException {
        callUpgrade(manager, horasEntreAtualizacao, false, false);
    }

    public static  void callUpgrade(DownloadManager manager, int horasEntreAtualizacao, boolean inicioDownloadAutomatico) throws IOException {
        callUpgrade(manager, horasEntreAtualizacao, inicioDownloadAutomatico, false);
    }

    public static  void callUpgrade(DownloadManager manager, int horasEntreAtualizacao, boolean inicioDownloadAutomatico, boolean inicioBackgound) throws IOException {
        final JDialog dlg = new JDialog(manager, "Verificando atualizações", true);
        JPanel panel = new JPanel(new BorderLayout());
        JProgressBar dpb = new JProgressBar(0, manager.getUrlList().size());
        JLabel jLabel = new JLabel("Verificando...");
//        dlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);


        panel.add(dpb,BorderLayout.NORTH);
        panel.add(jLabel,BorderLayout.SOUTH);

        dlg.add(panel, BorderLayout.CENTER);

        dlg.setSize(650, 40);
        dlg.setLocationRelativeTo(null);

        Thread t = new Thread(new Runnable() {
            public void run() {
                dlg.setVisible(true);
            }
        });

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        String titleAlert = "Download JW.ORG";
        scheduler.scheduleAtFixedRate(()->{
            manager.setStartTime(LocalDateTime.now());
            if(!inicioBackgound){
                t.start();
            }
            do {
                runVerify(manager, jLabel, dpb);
            } while (manager.hasException);
            dlg.dispose();
            manager.getDownloads().sort(Comparator.comparing(Download::getSize).reversed());
            manager.getDownloads().stream().filter(download -> !download.getStatus().equals(STATUS_DOWNLOAD.UPDATED)).forEach(download -> {
                manager.setTotalSize(manager.getTotalSize()+download.getSize());
                manager.tableModel.addDownload(download);
            });
            if(manager.getTableModel().getRowCount()>0){
                if(!inicioBackgound){
                    manager.show();
                }
            }else{
                logger.debug("Verificação encerrada em {} , tempo decorrido : {}"
                        , DateTimeFormatter.ofPattern(Download.SIMPLE_DATE_FORMAT.toPattern()).format( LocalDateTime.now())
                        , getTempoEntreDatas(manager.getStartTime(),  LocalDateTime.now())
                );
                if(!inicioBackgound){
                    JOptionPane.showMessageDialog(
                            null,
                            "Não há Atualizações nem novos vídeos disponíveis!",
                            titleAlert,
                            JOptionPane.WARNING_MESSAGE);
                }
            }
            IntStream.range(0, manager.getTableModel().getRowCount() ).forEach(value -> {
                ScheduledExecutorService scheduler2 = Executors.newScheduledThreadPool(1);
                scheduler2.scheduleAtFixedRate(()->{
                    int size  = IntStream.range(0, manager.getTableModel().getRowCount() )
                            .map(idx2->manager.getTableModel().getDownload(idx2).getSize()).sum();
                    int total = IntStream.range(0, manager.getTableModel().getRowCount() )
                            .map(idx2->manager.getTableModel().getDownload(idx2).getDownloaded()).sum();
                    if(total>0){
                        manager.setTotalSize(size);
                        manager.setTotalDownloaded(total);
                        manager.getTotalBar().setValue((int)manager.getProgress());
                        if (manager.getProgress()==100){
                            manager.getStartButton().setEnabled(false);
                            File folderToSave = new File(manager.getFolderToSave());
                            Download.removeAllFilesByFilter(folderToSave, ".part");
                            scheduler2.shutdown();
                            Desktop desktop = Desktop.getDesktop();
                            if(folderToSave.exists()) {
                                try {
                                    desktop.open(folderToSave);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }, 1, 500, TimeUnit.MILLISECONDS);
            });



            if(inicioDownloadAutomatico){
                manager.actionStart();
            }
        }, 0, horasEntreAtualizacao, TimeUnit.HOURS);
    }

    private static void runVerify(DownloadManager manager, JLabel jLabel, JProgressBar dpb){
        manager.setHasException(false);
        manager.setUrlVerified(0);
        manager.setStartTime(LocalDateTime.now());
        manager.getFilesToDownload().stream().forEach(file -> {
            try {
                Download download = new Download(verifyUrl(file.getProgressiveDownloadURL())
                        , manager.getFolderToSave(), file.getNameToSave());
                manager.getDownloads().add(download);
                manager.setUrlVerified(manager.getUrlVerified()+1);
                jLabel.setText(download.getFileName());
                dpb.setValue(manager.getUrlVerified());
            } catch (Exception e) {
                e.printStackTrace();
                manager.setHasException(true);
                return;
            }
        });
    }

    private int getConfirmOut(DownloadManager downloadManager, boolean inicioBackgound, int horasEntreAtualizacao){
        try {
            Thread.sleep((long)(Math.random() * 2500));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(downloadManager.isCloseOptionAlreadyOpen()){
            return -1;
        }else{
            logger.debug("Atualização encerrada em {} , tempo decorrido : {}"
                    , DateTimeFormatter.ofPattern(Download.SIMPLE_DATE_FORMAT.toPattern()).format( LocalDateTime.now())
                    , getTempoEntreDatas(downloadManager.getStartTime(),  LocalDateTime.now()));
            LocalDateTime now = LocalDateTime.now();
            logger.debug("Proxima Atualização programada para: {} , daqui : {}"
                    , DateTimeFormatter.ofPattern(Download.SIMPLE_DATE_FORMAT.toPattern()).format(now.plusHours(horasEntreAtualizacao))
                    , getTempoEntreDatas(now, now.plusHours(horasEntreAtualizacao) ));
            downloadManager.setCloseOptionAlreadyOpen(true);
            if(inicioBackgound){
                return -1;
            }
            return JOptionPane.showConfirmDialog(
                    null,
                    " Atualização feita com sucesso!\n Deseja Reiniciar o sistema ? ",
                    "Jkawflex Atualizações e melhorias",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
        }
    }



    /**
     * Habilita qualquer checagem de exceção com optional
     * @param resolver
     * @param <T>
     * @return
     */
    public static <T> Optional<T> resolveAnyException(Supplier<T> resolver) {
        try {
            T result = resolver.get();
            return Optional.ofNullable(result);
        }
        catch (Exception e) {
//            e.printStackTrace();
            return Optional.empty();
        }
    }


    /**
     * Restart the current Java application
     * @param runBeforeRestart some custom code to be run before restarting
     * @throws IOException
     */
    public static void restartApplication(Runnable runBeforeRestart) throws IOException {
        try {
            // java binary
            String java = System.getProperty("java.home") + "/bin/java";
            // vm arguments
            List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
            StringBuffer vmArgsOneLine = new StringBuffer();
            for (String arg : vmArguments) {
                // if it's the agent argument : we ignore it otherwise the
                // address of the old application and the new one will be in conflict
                if (!arg.contains("-agentlib")) {
                    vmArgsOneLine.append(arg);
                    vmArgsOneLine.append(" ");
                }
            }
// init the command to execute, add the vm args
            final StringBuffer cmd = new StringBuffer("\"" + java + "\" " + vmArgsOneLine);
            // program main and program arguments (be careful a sun property. might not be supported by all JVM)
            String[] mainCommand = System.getProperty("sun.java.command").split(" ");
            // program main is a jar
            if (mainCommand[0].endsWith(".jar")) {
                // if it's a jar, add -jar mainJar
                cmd.append("-jar " + new File(mainCommand[0]).getPath());
            } else {
                // else it's a .class, add the classpath and mainClass
                cmd.append("-cp \"" + System.getProperty("java.class.path") + "\" " + mainCommand[0]);
            }
            // finally add program arguments
            for (int i = 1; i < mainCommand.length; i++) {
                cmd.append(" ");
                cmd.append(mainCommand[i]);
            }
            // execute the command in a shutdown hook, to be sure that all the
            // resources have been disposed before restarting the application
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        String command = SystemUtils.IS_OS_UNIX? cmd.toString().replaceAll("\"", ""): cmd.toString();
                        logger.debug(command);
                        Runtime.getRuntime().exec(command);
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(
                                null,
                                "Erro ao Reiniciar o Sistema!\n ",
                                "Reniciar JKAWFLEZ",
                                JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }
                }
            });
            // execute some custom code before restarting
            if (runBeforeRestart != null) {
                runBeforeRestart.run();
            }
            // exit
            System.exit(0);
        } catch (Exception e) {
            // something went wrong
            JOptionPane.showMessageDialog(
                    null,
                    "Erro ao Reiniciar o Sistema!\n ",
                    "Reniciar JKAWFLEZ",
                    JOptionPane.ERROR_MESSAGE);
            throw new IOException("Error while trying to restart the application", e);
        }
    }

    public static String getTempoEntreDatas(LocalDateTime fromDateTime, LocalDateTime toDateTime ){
        LocalDateTime tempDateTime = LocalDateTime.from( fromDateTime );

        long hours = tempDateTime.until( toDateTime, ChronoUnit.HOURS);
        tempDateTime = tempDateTime.plusHours( hours );

        long minutes = tempDateTime.until( toDateTime, ChronoUnit.MINUTES);
        tempDateTime = tempDateTime.plusMinutes( minutes );

        long seconds = tempDateTime.until( toDateTime, ChronoUnit.SECONDS);

        return (hours>0? hours + " Horas, ":"") + (minutes>0? minutes + " Minutos e ":"") +   seconds + " Segundos " ;
    }


    public static String getTempoEntreDatas2(LocalDateTime fromDateTime, LocalDateTime toDateTime ){
        LocalDateTime tempDateTime = LocalDateTime.from( fromDateTime );

        long hours = tempDateTime.until( toDateTime, ChronoUnit.HOURS);
        tempDateTime = tempDateTime.plusHours( hours );

        long minutes = tempDateTime.until( toDateTime, ChronoUnit.MINUTES);
        tempDateTime = tempDateTime.plusMinutes( minutes );

        long seconds = tempDateTime.until( toDateTime, ChronoUnit.SECONDS);

        return (hours>0? StringUtils.leftPad(hours+"" ,2, "0") + ":":"") + (minutes>0? StringUtils.leftPad(minutes+"" ,2, "0") + ":":"00:") + StringUtils.leftPad(seconds+"" ,2, "0");
    }
}