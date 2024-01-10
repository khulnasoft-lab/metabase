git reset HEAD~1
rm ./backport.sh
git cherry-pick 4a9b1162c66f9f26675d61666a92c57c427359eb
echo 'Resolve conflicts and force push this branch'
