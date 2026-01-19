#!/bin/bash

# 1. Force clear any existing failed states
echo "Clearing local gcloud config cache..."
rm -rf ~/.config/gcloud/adc_control_client_info.json

# 2. Create the directory explicitly
mkdir -p ~/.config/gcloud/

# 3. Run the login command
# Added --no-quota-project-check to prevent the "serviceusage.services.use" error
# during the initial credential generation.
echo "Starting authentication..."
echo "COPY the URL below into your Windows Browser:"
echo "------------------------------------------------"

gcloud auth application-default login \
    --no-browser \
    --scopes="https://www.googleapis.com/auth/cloud-platform" \
    --no-quota-project-check

# 4. Check if the file was created in the Windows Profile instead
WINDOWS_ADC="/mnt/c/Users/$(cmd.exe /c "echo %USERNAME%" 2>/dev/null | tr -d '\r')/AppData/Roaming/gcloud/application_default_credentials.json"

if [ ! -f ~/.config/gcloud/application_default_credentials.json ]; then
    if [ -f "$WINDOWS_ADC" ]; then
        echo "Found credentials in Windows path. Linking to WSL..."
        cp "$WINDOWS_ADC" ~/.config/gcloud/application_default_credentials.json
    else
        echo "File still not found. Attempting manual write check..."
        touch ~/.config/gcloud/test_file && rm ~/.config/gcloud/test_file
        echo "Directory is writable. Please ensure you completed the browser flow and pasted the code."
    fi
fi

# 5. Final verification and Quota Project Fix
if [ -f ~/.config/gcloud/application_default_credentials.json ]; then
    echo "SUCCESS: Credential file located at ~/.config/gcloud/application_default_credentials.json"
    
    # Set the environment variable for the current session
    export GOOGLE_APPLICATION_CREDENTIALS="$HOME/.config/gcloud/application_default_credentials.json"
    
    echo "------------------------------------------------"
    echo "FIXING QUOTA PROJECT ISSUE:"
    echo "If you still see permission errors in your Java app for 'its-demo',"
    echo "ensure your account has the 'Service Usage Consumer' role on the project."
    echo "You can also try setting the quota project manually:"
    echo "gcloud auth application-default set-quota-project its-demo"
    echo "------------------------------------------------"
    echo "Add 'export GOOGLE_APPLICATION_CREDENTIALS=\"\$HOME/.config/gcloud/application_default_credentials.json\"' to your .bashrc"
else
    echo "ERROR: File was not created."
fi
