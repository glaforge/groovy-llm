import { createApp, ref } from 'https://unpkg.com/vue@3/dist/vue.esm-browser.js'
import { nanoid } from 'https://cdn.jsdelivr.net/npm/nanoid/nanoid.js';

createApp({
    setup: function () {
        const query = ref("");
        const chat = ref([]);
        const errorMsg = ref("");
        const chatId = nanoid(10);

        const sendQuery = async () => {
            chat.value.push({question: query.value});
            errorMsg.value = "";

            try {
                const response = await fetch("/query", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        "Accept": "application/json"
                    },
                    mode: "cors",
                    body: JSON.stringify({
                        query: query.value,
                        chatId: chatId
                    })
                });
                const questionAnswersOrError = await response.text();
                console.log(questionAnswersOrError);
                if (!response.ok) {
                    errorMsg.value = questionAnswersOrError;
                    throw new Error("[Response was not OK] " + questionAnswersOrError);
                }
                chat.value.push({ answer: questionAnswersOrError });
            } catch (e) {
                console.error(e);
            }

            query.value = '';
        };

        return {
            query,
            chat,
            sendQuery,
            errorMsg
        }
    }
}).mount('#app')