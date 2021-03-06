package settings;

/**
 * This enumerable type lists the various application-specific property types listed in the initial set of properties to
 * be loaded from the workspace properties <code>xml</code> file specified by the initialization parameters.
 *
 * @author Ritwik Banerjee
 * @see vilij.settings.InitializationParams
 */
public enum AppPropertyTypes {

    /* resource files and folders */
    DATA_RESOURCE_PATH,

    /* user interface icon file names */
    SCREENSHOT_ICON,

    /* tooltips for user interface buttons */
    SCREENSHOT_TOOLTIP,

    /* error messages */
    RESOURCE_SUBDIR_NOT_FOUND,

    /* application-specific message titles */
    SAVE_UNSAVED_WORK_TITLE,

    /* application-specific messages */
    SAVE_UNSAVED_WORK,

    /* application-specific parameters */
    DATA_FILE_EXT_NAME,
    DATA_FILE_EXT_DESC,
    SPECIFIED_FILE,
    LEFT_PANE_TITLE,
    LEFT_PANE_TITLEFONT,
    LEFT_PANE_TITLESIZE,
    CHART_TITLE,
    DUPLICATE_ERR_MSG,
    INCORRECT_FORMAT,
    DATA_FILE_LABEL,
    CHART_CSS,
    TEXTAREA_CSS,
    PNG_EXT,
    MAX_ITERATIONS,
    UPDATE_INTERVAL,
    CONTINUOUS_RUN,
    APPLY,
    NUM_CLUSTERS,
    RUN,
    CONFIGURATION,
    ALGORITHM_TYPE,
    CLASSIFICATION,
    CLUSTERING,
    RANDOM_CLASSIFICATION,
    RANDOM_CLUSTERING,
    FIELD_PROMPT_TEXT,
    EDIT,
    DONE,
    TSD_FULL_NAME,
    TSD_EXT,
    NULL,
    CONFIG_WINDOW_TITLE,
    CONFIG_ERROR_TITLE,
    CONFIG_ERROR_MESSAGE,
    DEFAULT_VALUE,
    EMPTY_STRING,
    STATS_WITHOUT_PATH,
    STATS_WITH_PATH,
    CHART_SERIES_LINE,
    AVG_SERIES,
    NULL_STROKE,
    AVG_SERIES_STROKE_WIDTH,
    CHART_LINE_SYMBOL,
    AVG_SERIES_BG_COLOR,
    EXIT_WHILE_RUNNING_WARNING,
    UNFINISHED_RUN,
    UNFINISHED_NEWDATA_WARNING,
    UNFINISHED_SAVE_WARNING,
    UNFINISHED_LOAD_WARNING,
    RANDOM_CLASSIFIER_LINE,
    RANDOMCLASSIFIER,
    RANDOMCLUSTERER,
    KMEANSCLUSTERER,
    JAVA_EXT,
    ALGORITHMS_REL_PATH,
    CLASSIFIER,
    CLUSTERER,
    K_MEANS_CLUSTERING
}
