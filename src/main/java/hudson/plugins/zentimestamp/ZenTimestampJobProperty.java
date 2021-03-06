package hudson.plugins.zentimestamp;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;


public class ZenTimestampJobProperty extends JobProperty<Job<?, ?>> {

    private boolean changeBUILDID;

    private String pattern;

    @DataBoundConstructor
    public ZenTimestampJobProperty(boolean changeBUILDID, String pattern) {
        this.changeBUILDID = changeBUILDID;
        this.pattern = pattern;
    }

    @SuppressWarnings("unused")
    public String getPattern() {
        return pattern;
    }

    @SuppressWarnings("unused")
    public boolean isChangeBUILDID() {
        return changeBUILDID;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    @Extension
    @SuppressWarnings("unused")
    public static final class DescriptorImpl extends JobPropertyDescriptor {

        public DescriptorImpl() {
            super(ZenTimestampJobProperty.class);
            load();
        }

        public boolean isApplicable(Class<? extends Job> jobType) {
            return AbstractProject.class.isAssignableFrom(jobType);
        }

        public String getDisplayName() {
            return Messages.ZenTimestampFormatBuildWrapper_displayName();
        }


        @SuppressWarnings("unchecked")
        public ZenTimestampJobProperty newInstance(org.kohsuke.stapler.StaplerRequest req, net.sf.json.JSONObject jsonObject) throws Descriptor.FormException {
            String pattern = null;

            //Get the zentimestamp jobproperty, it's in piority
            Object changeBUILDID = jsonObject.get("changeBUILDID");
            try {

                if (changeBUILDID != null) {
                    pattern = ((JSONObject) changeBUILDID).getString("pattern");
                    if ((pattern != null) && (pattern.trim().length() != 0)) {
                        //Desactive the build wrapper
                        ZenTimestampFormatBuildWrapper.backwardCompatibility = false;
                        //Create a new job property object
                        return new ZenTimestampJobProperty(true, pattern);
                    }
                }

                // Only for previous job (configured before zentimestamp < 2.0)
                if (ZenTimestampFormatBuildWrapper.isConfigXMLWithPreviousVersion()) {
                    //Retrieve the current job by its name
                    String jobName = (String) req.getSubmittedForm().get("name");
                    TopLevelItem topLevelItem = Hudson.getInstance().getItem(jobName);

                    // Retrieve the previous job zentimestamp wrapper pattern
                    Map<Descriptor<BuildWrapper>, BuildWrapper> mapWrappers = ((Project) (Items.getConfigFile(Hudson.getInstance().getItem(jobName)).read())).getBuildWrappers();
                    for (Map.Entry<Descriptor<BuildWrapper>, BuildWrapper> entry : mapWrappers.entrySet()) {
                        BuildWrapper wrapper = entry.getValue();
                        if (wrapper.getClass() == ZenTimestampFormatBuildWrapper.class) {
                            pattern = ((ZenTimestampFormatBuildWrapper) wrapper).getPattern();
                        }
                    }
                    //Desactive the build wrapper
                    ZenTimestampFormatBuildWrapper.backwardCompatibility = false;
                    //Create a new job property object
                    return new ZenTimestampJobProperty(true, pattern);
                }
            } catch (Exception e) {
                throw new RuntimeException("An error occurred during the migration of the previous plugin");
            }

            return null;
        }

        @SuppressWarnings("unused")
        public FormValidation doCheckPattern(@QueryParameter String value) {

            if (value == null || value.trim().length() == 0) {
                return FormValidation.error(Messages.ZenTimestampFormatBuildWrapper_emptyPattern());
            }

            try {
                new SimpleDateFormat(value);
            } catch (NullPointerException npe) {
                return FormValidation.error(Messages.ZenTimestampFormatBuildWrapper_invalidInput(npe.getMessage()));
            } catch (IllegalArgumentException iae) {
                return FormValidation.error(Messages.ZenTimestampFormatBuildWrapper_invalidInput(iae.getMessage()));
            }

            return FormValidation.ok();
        }

    }

}
