echo "Before continuing!"
echo
echo "This script will execute *all* the experiments of the “Logical and Physical Optimizations for SQL Query Execution over Large Language Models” using TogetherAI as the LLM provider."
echo
echo "The model to use should have been configured in the <configuration.properties> file using the <togetherai.model> key. Also, the TogetherAI api key should have been properly added to the <configuration.properties> file using the <togetherai.api-key> key."
echo
read -p "Start the execution of *all* the experiments? (y/N): " confirm && [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]] || exit 1

cd ../executors
names=`ls ./*-togetherai.sh`
for executor in $names
do
  sh $executor
done
:
