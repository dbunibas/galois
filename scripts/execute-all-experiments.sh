echo "TODO: warning..."
read -p "Continue? (y/N): " confirm && [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]] || exit 1

cd ../executors
names=`ls ./*.sh`
for executor in $names
do
  sh $executor
done
:
