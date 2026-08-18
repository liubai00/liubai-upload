new Vue({
    el: '#app',
    data: {
        file: null,
        sha256: null,
        startByte: 0,
        totalBytes: 0,
        uploadedBytes: 0,
        uploadFileSHA256: '',
        currentSha256: '',
    },
    methods: {

        /**
         * 处理文件选择事件
         * @param event 文件选择事件
         */
        handleFileChange(event) {
            this.file = event.target.files[0];
            this.totalBytes = this.file.size;
        },

        /**
         * 处理开始上传按钮点击事件
         * @returns {Promise<void>}
         */
        async startUpload() {
            try {
                // 计算文件的SHA256值
                this.sha256 = await this.calculateFileSHA256(this.file);
                console.log('SHA256:', this.sha256);

                // 在这里发起上传预处理请求
                await this.preprocessFileUpload();
            } catch (error) {
                console.error('Start upload error:', error);
                alert('上传失败')
            }
        },

        /**
         * 计算文件的SHA256值
         */
        calculateFileSHA256(file) {
            return new Promise((resolve, reject) => {
                const reader = new FileReader();
                const blockSize = 1024 * 1024; // 1MB
                let start = 0;
                let sha256 = CryptoJS.algo.SHA256.create();

                reader.onload = (e) => {
                    const fileData = new Uint8Array(e.target.result);
                    const wordArray = CryptoJS.lib.WordArray.create(fileData);
                    sha256.update(wordArray);

                    if (start < file.size) {
                        // 继续读取下一个块
                        readNextBlock();
                    } else {
                        // 完成所有块的读取
                        let sha256Str = sha256.finalize().toString();
                        resolve(sha256Str);
                    }
                };

                reader.onerror = (error) => {
                    console.error('FileReader error:', error);
                    reject(error);
                };

                const readNextBlock = () => {
                    const end = Math.min(start + blockSize, file.size);
                    reader.readAsArrayBuffer(file.slice(start, end));
                    start = end;
                }

                // 开始读取第一个块
                readNextBlock();
            });
        },


        /**
         * 上传预处理
         * @returns {Promise<void>}
         */
        preprocessFileUpload() {
            try {
                axios.get('http://localhost:7878/api/file/preprocess', {
                    params: {
                        sha256: this.sha256,
                        totalBytes: this.totalBytes,
                    },
                }).then(async (response) => {
                    console.log(response);

                    // 处理预处理的响应
                    if (response.data.code === 20000) {
                        // 提取相关参数
                        this.uploadedBytes = response.data.data.uploadedBytes;
                        this.uploadFileSHA256 = response.data.data.currentSha256;

                        // 在这里可以根据返回的参数进行相应的处理
                        console.log('Uploaded Bytes:', this.uploadedBytes);
                        console.log('uploadFile SHA256:', this.uploadFileSHA256);

                        // 根据上传的情况决定是否继续上传文件
                        if (this.uploadFileSHA256 === this.sha256 && this.uploadedBytes === this.totalBytes) {
                            // 文件已经上传完成
                            alert('文件已存在');
                            return;
                        }

                        if (this.uploadedBytes === 0) {
                            // 从头开始上传
                            this.startByte = 0;
                            this.uploadFile();
                        } else if (this.uploadedBytes > 0) {
                            // 从已上传的部分开始上传
                            // 切割文件，获取已上传和未上传的部分
                            const uploadedPart = this.file.slice(0, this.uploadedBytes);
                            const unUploadedPart = this.file.slice(this.uploadedBytes);
                            console.log('Uploaded Part:', uploadedPart);
                            console.log('UnUploaded Part:', unUploadedPart);

                            // 计算已上传部分的SHA256值
                            const uploadedPartSha256 = await this.calculateFileSHA256(uploadedPart)
                            console.log('Uploaded Part SHA256:', uploadedPartSha256, this.uploadFileSHA256)

                            // 将计算出的SHA256值与服务器返回的SHA256值进行比较
                            if (uploadedPartSha256 === this.uploadFileSHA256) {
                                // 如果匹配，继续上传未上传的部分
                                this.file = unUploadedPart;
                                this.startByte = this.uploadedBytes;
                                this.uploadFile();
                            } else {
                                // 如果不匹配，从头开始上传
                                this.startByte = 0;
                                this.uploadFile();
                            }

                        }

                    } else {
                        console.error('Preprocess failed:', response.data.message);
                    }
                }).catch((error) => {
                    console.error('error:', error);
                });


            } catch (error) {
                console.error(error);
            }
        },

        /**
         * 上传文件
         * @returns {Promise<void>}
         */
        uploadFile() {
            const reader = new FileReader();
            reader.onload = (e) => {
                const fileData = e.target.result;
                const blob = new Blob([fileData], {type: this.file.type});
                const formData = new FormData();
                formData.append('file', blob);
                formData.append('sha256', this.sha256);
                formData.append('startByte', this.startByte);
                formData.append('totalBytes', this.totalBytes);

                axios({
                    method: 'post',
                    url: 'http://localhost:7878/api/file/upload',
                    data: formData,
                    headers: {
                        'Content-Type': 'multipart/form-data'
                    },
                }).then(response => {
                    console.log(response);

                    // 处理上传响应
                    if (response.data.code === 20000) {
                        // 弹出弹窗，上传成功
                        alert('上传成功');
                    } else {
                        console.error('Upload failed:', response.data.message);
                    }
                }).catch(error => {
                    console.error('error:', error);
                })
            };
            reader.readAsArrayBuffer(this.file);
        },

    },
});
