echo "Before continuing!"
echo
echo "This script will execute *all* the experiments of the “Logical and Physical Optimizations for SQL Query Execution over Large Language Models” using OpenAI as the LLM provider."
echo
echo "The model to use should have been configured in the <configuration.properties> file using the <openai.model-name> key. Also, the OpenAI api key should have been properly added to the <configuration.properties> file using the <openai.api-key> key."
echo
read -p "Start the execution of *all* the experiments? (y/N): " confirm && [[ $confirm == [yY] || $confirm == [yY][eE][sS] ]] || exit 1

cd ../executors
names=`ls ./*-openai.sh`
for executor in $names
do
  sh $executor
done
:
