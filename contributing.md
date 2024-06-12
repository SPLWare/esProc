The esProc project welcomes all contributors. This document is a guide to help you through the process of contributing to esProc development and maintenance.

# 1 Copyright notice

Once you submit your code and documents to the esProc open-source project, you grant the recipient of the esProc software a perpetual, global, non-exclusive, free, copyright-free and irrevocable copyright license to use, copy, modify, sublicense and distribute your contributions and their derivative works. The use of your content will not cause harm to any person or entity, and you shall compensate for any damages caused by your content. The esProc project will not be responsible for any content provided by you or any third party.

# 2 Becoming a contributor

First you need to register a GitHub account and then follow these required steps. If you are already familiar with the GitHub registering process, just ignore this section.

## 2.1 Download code to the local

### (1)Fork repository

Open<a href="https://github.com/SPLWare/esProc" target="_blank"> SPLWare/esProc </a>GitHub homepage, click Fork button to create a repository copy under your username, such as <https://github.com/USERNAME/esProc>

### (2)Clone the remote repository to local

git clone <git@github.com>:/USERNAME/esProc.git

## 2.2 Compile on your local machine

You can use the common Java development tools to import esProc as a Maven project. Take Eclipse as an example, we import it to an existing Maven project.
![comimg001](https://img.raqsoft.com.cn/file/2024/06/a194059662b640f69e608fefb8d4b741_conimg001.png)
Next, select the directory where the esProc project is stored.
![comimg002](https://img.raqsoft.com.cn/file/2024/06/25c2a1c96e7f42a598a817b5453fa833_conimg002.png)
Click “Finish” button and wait for the project to load and compile automatically.
![comimg003](https://img.raqsoft.com.cn/file/2024/06/38632f82dd4a417c978e8249112b2541_conimg003.png)

## 2.3 Commit code

### (1)Keep a repository updated locally

Before creating a Pull Request to merge code into the original esProc repository (<https://github.com/SPLWare/esProc>), you need to synchronize the latest code from the original repository.

The remote repository named “origin” is the esProc repository that you forked to your own username. Next, you need to create a remote host of the original esProc repository and name it “upstream”.

git remote add upstream <git@github.com>\:SPLWare/esProc.git

Get the latest code in the original esProc repository.

git pull upstream master

### (2)Commit changes locally

Before executing git commit, use git add \<file> to add the modified or newly added files. If you want to undo the changes to a file, use git checkout -- \<file>.

Each git commit requires a commit comment to help others understand what changes were made in each commit. This can be done with git commit -m "My comment"

### (3)Push commits to a remote repository

Push your local changes to GitHub, that is, <https://github.com/USERNAME/esProc>.

git push origin master

### (4)Merge a Pull Request (PR) to the full commits

Open the page <https://github.com/USERNAME/esProc>, switch to Pull requests, and click “New pull request” button.
![comimg004](https://img.raqsoft.com.cn/file/2024/06/eb6f852e039847359c4076969039a052_conimg004.png)
Add the necessary comments and click “Create pull request” button.

## 2.4 Reply Code Review and revise changes

Please reply a comment by starting a review, rather than replying directly in the PR comment box. The latter will result in one email each time you reply.

# 3.Learning, communicating and sharing feedback

If you have new feature requirements or find bugs, you can create an issue on GitHub to give feedback. If you need more information, go to <https://www.scudata.com/>, where you can find a large amount of documentation as well as initiating or participating in various discussions on the forum.

Due to time zone differences, you may not get an immediate response, so please be patient.

# 4. Specifications and references

## 4.1 Coding style guide

Refer to the source code that comes with JDK, and there are no other requirements.

## 4.2 Error message specification

Describe input and errors directly, including full exception stack trace (if any);

Provide a complete comparison of expectations and actual performance;

If you have any suggestions for changes, add it to the description.
