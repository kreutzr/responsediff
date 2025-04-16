# =================================================================
# Script to compile all relevant files into the "releases" folder.
# The new created subfolder by then be zipped.
# =================================================================
# -------------------------------------------------------
# Build the new version
# -------------------------------------------------------
mvn clean install -P dependencyCheck -P license-summary

# -------------------------------------------------------
# Switch to "releases" folder
# -------------------------------------------------------
cd ./releases
mkdir ./ResponseDiff

# -------------------------------------------------------
# Delete existing file compilation of former version
# -------------------------------------------------------
rm ./ResponseDiff/*
mkdir ./ResponseDiff/doc
mkdir ./ResponseDiff/reporter

# -------------------------------------------------------
# Create file compilation of new version
# -------------------------------------------------------
cp ../target/responsediff-*               ./ResponseDiff/.
cp ../target/log4j2.xml                   ./ResponseDiff/.
cp ../LICENSE                             ./ResponseDiff/.
cp ../target/dependency-check-report.html ./ResponseDiff/.
cp -r ../target/doc/*                     ./ResponseDiff/doc/.
rm -rf ./ResponseDiff/doc/src
cp -r ../target/reporter/*                ./ResponseDiff/reporter/.

cd -

# -------------------------------------------------------
# Ready to be zipped and renamed
# -------------------------------------------------------

echo "done"
