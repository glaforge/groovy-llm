import {createApp, nextTick, ref} from 'https://unpkg.com/vue@3/dist/vue.esm-browser.js'
import { nanoid } from 'https://cdn.jsdelivr.net/npm/nanoid/nanoid.js';

createApp({
    setup: function () {
        const query = ref("");
        const chat = ref([]);
        const errorMsg = ref("");
        const ongoingCall = ref(false);
        const chatId = nanoid(10);

        const sendQuery = async () => {
            chat.value.push({question: query.value});
            errorMsg.value = "";

            try {
                ongoingCall.value = true;
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
                if (response.ok) {
                    chat.value.push({answer: questionAnswersOrError});
                    await nextTick();
                    hljs.highlightAll();
                } else {
                    errorMsg.value = JSON.parse(questionAnswersOrError)._embedded.errors[0].message;
                }
            } catch (e) {
                console.error(e);
            } finally {
                ongoingCall.value = false;
            }
            query.value = '';
        };

        return {
            query,
            chat,
            sendQuery,
            errorMsg,
            ongoingCall
        }
    }
}).mount('#app')